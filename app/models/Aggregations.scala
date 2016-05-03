package models

import com.github.jeroenr.bson.BsonDocument
import com.github.jeroenr.bson.BsonDsl._
import helpers.BsonDocumentHelper._
import play.api.libs.json.{JsResult, Json}

/**
  * Created by: MAXIRU 
  * Date: 3/30/16
  * Time: 11:59 AM
  */

case class StepSummary(
                        _id: Int,
                        stepStarted: Int,
                        stepRestarted: Int,
                        stepCompleted: Int,
                        stepStalled: Int,
                        timeMin: Int,
                        timeMax: Int
                      )

object StepSummary {
  implicit val stepsFormat = Json.format[StepSummary]

  def apply(bson: BsonDocument): JsResult[StepSummary] = {
    Json.fromJson[StepSummary](bson)
  }

  def toBsonDocument(steps: StepSummary): BsonDocument =
    ("_id" := steps._id) ~
      ("stepStarted" := steps.stepStarted) ~
      ("stepRestarted" := steps.stepStarted) ~
      ("stepCompleted" := steps.stepCompleted) ~
      ("stepStalled" := steps.stepStalled) ~
      ("timeMin" := steps.timeMin) ~
      ("timeMax" := steps.timeMax)
}

case class TerminalStep(step: Int, ts: Long)

object TerminalStep {
  implicit val termminalStepFormat = Json.format[TerminalStep]
  def apply(bson: BsonDocument): JsResult[TerminalStep] = {
    Json.fromJson[TerminalStep](bson)
  }
}

case class Terminal(_id: String, terminalSteps: List[TerminalStep])

object Terminal {
  implicit val terminalFormat = Json.format[Terminal]
  def apply(bson: BsonDocument): JsResult[Terminal] = {
    Json.fromJson[Terminal](bson)
  }
}

case class QuickStats(started: Int, stalled: Int, restarted: Int, finished: Int)

object QuickStats {
  implicit val statsFormat = Json.format[QuickStats]
}

case class ClientPayload(stepSummaries: List[StepSummary], quickStats: QuickStats, versions: List[String])

case object ClientPayload {
  implicit val payloadFormat = Json.format[ClientPayload]
}