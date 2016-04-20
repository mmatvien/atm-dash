package controllers

import javax.inject.Inject

import akka.stream.Materializer
import flows.Flow1
import models.{ATMEvent, ATMEventRepo, ClientPayload}
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.ExecutionContext


class Application @Inject()(wSClient: WSClient, val atmEventRepo: ATMEventRepo)(implicit ec: ExecutionContext, mat: Materializer) extends Controller with Flow1 {

  def index = Action {
    //default version
    val defaultVersion = atmEventRepo.versions.reverse.head
    Redirect(routes.Application.version(defaultVersion))
  }


  def version(version: String)  = Action.async {
    // init the feed
    val clientPayload = atmEventRepo.initializeClient(version)
    clientPayload.map { s => println(s); Ok(views.html.index(s, version)) }
  }


  def feed = Action {
    Ok.chunked(atmEventRepo.source.map { p => println(s"to client: $p"); Json.toJson[ClientPayload](p) } via EventSource.flow)
  }


  def saveEvent = Action(BodyParsers.parse.json) { request =>
    val atmEventResult = request.body.validate[ATMEvent]
    atmEventResult.fold(
      errors => {
        BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
      },
      atmEvent => {
        atmEventRepo.insert(List(atmEvent))
        Ok(Json.obj("status" -> "OK", "message" -> ("Place '" + atmEvent.message + "' saved.")))
      }
    )
  }
}