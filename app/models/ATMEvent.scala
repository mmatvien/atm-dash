package models

import java.util.Date
import javax.inject.Inject

import actors.DataPublisher.Publish
import actors.{DataPublisher, RouterActor}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.github.jeroenr.bson.BsonDocument
import com.github.jeroenr.bson.BsonDsl._
import com.github.jeroenr.bson.element.BsonObjectId
import helpers.BsonDocumentHelper._
import play.api.libs.json.{JsResult, JsSuccess, Json}
import play.api.modules.tepkinmongo.TepkinMongoApi

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by: MAXIRU 
  * Date: 3/29/16
  * Time: 9:45 AM
  */

case class ATMEvent(
                     _id: Option[String],
                     version: String,
                     step: Int,
                     message: String,
                     ts: Long,
                     off: Int,
                     term: String,
                     ip: String)

object ATMEvent {
  implicit val eventFormatter = Json.format[ATMEvent]
  def apply(bson: BsonDocument): JsResult[ATMEvent] = {
    Json.fromJson[ATMEvent](bson)
  }

  def toBsonDocument(atmEvent: ATMEvent): BsonDocument =
    ("_id" := BsonObjectId.generate) ~
      ("version" := atmEvent.version) ~
      ("step" := atmEvent.step) ~
      ("message" := atmEvent.message) ~
      ("ts" := atmEvent.ts) ~
      ("off" := atmEvent.off) ~
      ("term" := atmEvent.term) ~
      ("ip" := atmEvent.ip)
}

case class Collection(name: String)

object Collection {
  implicit val collectionFormatter = Json.format[Collection]
  def apply(bson: BsonDocument): JsResult[Collection] = {
    Json.fromJson[Collection](bson)
  }

  def toBsonDocument(atmEvent: Collection): BsonDocument =
    "name" := atmEvent.name
}

class ATMEventRepo @Inject()(tepkinMongoApi: TepkinMongoApi, system: ActorSystem)(implicit mat: Materializer) {
  implicit val ec = tepkinMongoApi.client.ec
  implicit val timeout: Timeout = 5.seconds
  var tailedCollections: List[String] = List.empty[String]
  var versions: List[String] = List.empty[String]
  var clientConnected = false

  println(" --------------------- CREATED ATM EVENT REPO ---------")

  val router: ActorRef = system.actorOf(Props[RouterActor], "router")
  val source = Source.actorPublisher[ClientPayload](Props(classOf[DataPublisher], router))

  val db = tepkinMongoApi.client("atm")

  initialize()

  def initialize() = {
    val result = for {
      source <- db.listCollections()
      collections <- source.runFold(List.empty[BsonDocument])(_ ++ _)
    } yield collections
    result.map { collections =>
      collections.map {Collection(_)}.collect {
        case JsSuccess(c, _) =>
          val name = c.name
          if (!name.contains('$') && name.contains("events_")) {
            val collectionName = name.substring(name.indexOf('.') + 1)
            val versionString = name.substring(name.indexOf('_') + 1)
            versions = versionString :: versions
            println(s"$collectionName $versionString")
            tailCollection(versionString)
          }
      }
    }
  }


  def generateQuickStats(st: List[StepSummary]): QuickStats = {
    val started = st.find(_._id == 1).getOrElse(StepSummary(1, 0, 0, 0, 0, 0, 0)).stepStarted
    val stalled = st.foldLeft(0)((acc, st) => acc + st.stuckCount)
    val restarted = st.foldLeft(0)((acc, st) => acc + st.restartedCount)
    val finished = st.count(_._id == 142)
    QuickStats(started, stalled, restarted, finished)
  }

  def tailCollection(version: String): Unit = {

    println(s"tailed collections = $tailedCollections")
    if (tailedCollections.contains(version))
      println(s"collection $version is already tailed")
    else {
      println(s"creating collection events_$version")
      if (!versions.contains(version))
        versions = version :: versions
      tailedCollections = version :: tailedCollections
      db.createCollection(s"events_$version", capped = Some(true), size = Some(100000000))

      val collection = db(s"events_$version")
      collection
        .find(query = BsonDocument.empty, tailable = true)
        .runForeach {
          event => event.map(ATMEvent(_)).collect {
            case JsSuccess(p, _) =>
              if (clientConnected) {
                val payload = constructPayload(version)
                payload.map { st =>
                  val quickStats = generateQuickStats(st)
                  router ! Publish(ClientPayload(st, quickStats, versions))
                }
              }
          }
        }
    }
  }

  def constructPayload(version: String): Future[List[StepSummary]] = {
    //    val stepsF = groupSteps(version)
    val termsF = groupTerminals(version)

    for {
      terms <- termsF
    } yield {
      println(terms)
      for {
        steps <- terms.map(_.terminalSteps)
        step <- steps
      } yield {
        val stepCompletionT = stepCompletionTimes(step.step, terms)
        val minMax = stepCompletionT.foldLeft((0, 0))((acc, t) => (acc._1 min t._3, acc._2 max t._3))
        val (started, restarted, completed) = calculateStepStarted(step.step, terms)

        StepSummary(
          step.step,
          started,
          completed,
          minMax._1,
          minMax._2,
          calculateStuckCount(step.step, terms),
          restarted
        )
      }
    }.distinct
  }


  def calculateStepStarted(stepNumber: Int, terms: List[Terminal]): (Int, Int, Int) = {
    // count started once and accumulate the restarts if same step was started more then once

    terms.foldLeft((0, 0, 0)) {
      (acc, term) =>
        val terminalStepFiltered = term.terminalSteps.filter(_.step == stepNumber)
        val terminalStepCount = terminalStepFiltered.length

        val stepCompleted = term.terminalSteps.count(st => st.step == stepNumber + 1 )

        if (terminalStepCount > 1) {
          // restarted
          (acc._1 + 1, acc._2 + (terminalStepCount - 1), acc._3 + (stepCompleted - (terminalStepCount - 1)))
        } else {
          (acc._1 + terminalStepCount, acc._2, acc._3 + stepCompleted)
        }
    }
  }


  def stepCompletionTimes(step: Int, terms: List[Terminal]): List[(String, Int, Int)] = {
    terms.map { term =>

      val stepStartedList = term.terminalSteps.filter(_.step == step)
      val lastTs = stepStartedList.reverse.head.ts

      val stepEndedList = term.terminalSteps.filter(_.step == step + 1)

      val timePassed =
        if (stepEndedList.isEmpty) {
          // still working ...
          (new Date().getTime / 1000) - lastTs
        } else {
          stepEndedList.head.ts - lastTs
        }
      (term._id, step, timePassed.toInt)
    }
  }

  def calculateStuckCount(step: Int, terms: List[Terminal]) = {
    1
  }


  def initializeClient(version: String) = {
    println(" --------------------- CLIENT INITIALIZED ---------")
    clientConnected = true
    constructPayload(version).map(st => ClientPayload(st, generateQuickStats(st), versions))
  }


  def groupTerminals(version: String) = {
    val collection = db(s"events_$version")

    val pipeline: List[BsonDocument] = List(
      "$match" := BsonDocument.empty,
      "$group" := ("_id" := "$term") ~
        ("terminalSteps" := {"$push" := ("step" := "$step") ~ ("ts" := "$ts")}) ~
        ("total" := ("$sum" := 1)),
      "$sort" := ("total" := -1)
    )

    val res = collection.aggregate(pipeline).runFold(List.empty[BsonDocument])(_ ++ _)

    res.map { re =>
      re.map {Terminal(_)}.collect {
        case JsSuccess(p, _) => p
      }
    }
  }


  def insert(ps: List[ATMEvent]) = {
    val version = ps.head.version

    val now = new Date()
    val epoch = now.getTime / 1000

    if (!tailedCollections.contains(version)) {
      tailCollection(version)
      db(s"events_$version").insert(ps.map(event => ATMEvent.toBsonDocument(event.copy(ts = epoch))))
    } else {
      db(s"events_$version").insert(ps.map(event => ATMEvent.toBsonDocument(event.copy(ts = epoch))))
    }
  }

}