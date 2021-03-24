package key_value_store.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finagle.http.Status
import com.twitter.inject.server.FeatureTest
import key_value_store._

// Note that the feature test here uses a single server state
// Test runs that write data are expected to clean up after themselves
class KeyValueServerTest extends FeatureTest {
  override val server = new EmbeddedHttpServer(
    twitterServer = new KeyValueServer
  )

  val serializer: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  test("KeyValueServer should return empty when no key exists") {
    val key = "test"
    get(key, andExpect = Status.NotFound) shouldBe Some(s"Key not found: $key")
  }

  test("KeyValueServer should save, retrieve, and delete a key") {
    val key = "test"
    val value = "ok"

    set(key, value) shouldBe (key, value)

    get(key) shouldBe Some(value)

    delete(key) shouldBe key

    get(key, andExpect = Status.NotFound) shouldBe Some(s"Key not found: $key")
  }

  test("KeyValueServer should return error message with 200 when deleting a non-existent key") {
    delete("test") shouldBe "Key did not exist: test"
  }

  test("KeyValueServer should update values") {
    val key = "key"

    val oldValue = "test"
    val newValue = "testing"

    set(key, oldValue) shouldBe (key, oldValue)
    get(key) shouldBe Some(oldValue)

    set(key, newValue) shouldBe (key, newValue)
    get(key) shouldBe Some(newValue)
  }

  test("KeyValueServer should error when called improperly") {
    server.httpGet(path = "/get", andExpect = Status.BadRequest)
    server.httpPost(path = "/set", postBody = "{}", andExpect = Status.BadRequest)
    server.httpPost(path = "/set", postBody = """{"key": "test"}""", andExpect = Status.BadRequest)
    server.httpPost(path = "/set", postBody = """{"value": "test"}""", andExpect = Status.BadRequest)
    server.httpPost(path = "/delete", postBody = """{"value": "test"}""", andExpect = Status.BadRequest)
  }

  private def get(key: String, andExpect: Status = Status.Ok): Option[String] = {
    val resp = server.httpGet(path = s"/get?key=$key", andExpect = andExpect)
    if (resp.status == Status.Ok) {
      Some(serializer.readValue(resp.contentString, classOf[Entry]).value)
    } else {
      serializer.readValue(resp.contentString, classOf[Errors]).errors.headOption
    }
  }

  private def set(key: String, value: String, expectedStatus: Status = Status.Ok): (String, String) = {
    val body = Entry(key, value)
    val resp = server.httpPost(path = "/set", postBody = serializer.writeValueAsString(body), andExpect = expectedStatus)
    val typed = serializer.readValue(resp.contentString, classOf[Entry])
    (typed.key, typed.value)
  }

  private def delete(key: String, expectedStatus: Status = Status.Ok): String = {
    val body = DeleteRequestResponse(key)
    val resp = server.httpPost(path = "/delete", postBody = serializer.writeValueAsString(body), andExpect = expectedStatus)
    serializer.readValue(resp.contentString, classOf[DeleteRequestResponse]).key
  }
}
