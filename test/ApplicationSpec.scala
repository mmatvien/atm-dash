import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  * For more information, consult the wiki.
  */
//@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpec with OneAppPerSuite {


  // Override app if you need a Application with other than
  // default parameters.
  implicit override lazy val app = new GuiceApplicationBuilder().configure(Map("ehcacheplugin" -> "disabled")).build()


  "Application" should {

    "send 404 on a bad request" in {


      def rand() = {
        2
      }

      val request = FakeRequest(POST, "/events").withJsonBody(Json.parse(
        s"""{
          | "version": "1.2" , "step": ${rand()}, "message": "STEP MESSAGE", "ts": 123, "off": 0, "term": "terminal123", "ip": "123.123.123.2" }""".stripMargin))


      val result = route(app, request)


      status(result.get) mustEqual OK
    }

  }
}
