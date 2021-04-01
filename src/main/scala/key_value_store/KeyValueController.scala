package key_value_store

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.annotations.QueryParam

import javax.inject.Inject
import scala.collection.mutable

// file checkpointing

case class Value(value: String, expirationTimestamp: Option[Long])

class KeyValueController @Inject() extends Controller {
  private val entries = mutable.HashMap[String, Value]()
  private val serializer: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  post("/set") { req: SetRequest =>
    val ttl = req.ttl.map(ttl => ttl * 1000 + System.currentTimeMillis())

    val value = Value(req.value, ttl)
    entries.put(req.key, value)
    req
  }

  get("/get") { req: GetRequest =>
    entries.get(req.key)
      .filter(v => {
        val isValid = v.expirationTimestamp.forall(_ > System.currentTimeMillis())
        // If the timestamp is expired, remove it from the map
        if (!isValid) {
          entries.remove(req.key)
        }
        isValid
      })
      .map(v => SetRequest(req.key, v.value, v.expirationTimestamp))
      .getOrElse {
        response.notFound(Errors(List(s"Key not found: ${req.key}")))
      }
  }

  post("/delete") { req: DeleteRequestResponse =>
    val resp = entries.remove(req.key)
      .map(_ => req.key)
      .getOrElse(s"Key did not exist: ${req.key}")

    DeleteRequestResponse(resp)
  }
}

case class SetRequest(
  key: String,
  value: String,
  ttl: Option[Long]
) {
  def asEntry: Entry = {
    Entry(
      key = key,
      value = value
    )
  }
}

case class Entry(
  key: String,
  value: String
)

case class GetRequest(
  @QueryParam key: String
)

case class DeleteRequestResponse(
  key: String
)

// This keeps parity with the default error handling by Finatra
case class Errors(
  errors: List[String]
)