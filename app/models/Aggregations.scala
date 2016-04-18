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

case class StepSummary(_id: Int,
                 stepStarted: Int,
                 stepCompleted: Int,
                 timeMin: Int,
                 timeMax: Int,
                 stuckCount: Int,
                 restartedCount: Int)

object StepSummary {
  implicit val stepsFormat = Json.format[StepSummary]

  def apply(bson: BsonDocument): JsResult[StepSummary] = {
    Json.fromJson[StepSummary](bson)
  }

  def toBsonDocument(steps: StepSummary): BsonDocument =
    ("_id" := steps._id) ~
      ("stepStarted" := steps.stepStarted) ~
      ("stepCompleted" := steps.stepCompleted) ~
      ("timeMin" := steps.timeMin) ~
      ("timeMax" := steps.timeMax) ~
      ("stuckCount" := steps.stuckCount) ~
      ("restartedCount" := steps.restartedCount)
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

case class ClientPayload(stepSummaries: List[StepSummary])

case object ClientPayload {
  implicit val payloadFormat = Json.format[ClientPayload]
}