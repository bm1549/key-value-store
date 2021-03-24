package key_value_store

import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.annotations.QueryParam

import javax.inject.Inject
import scala.collection.mutable

class KeyValueController @Inject() extends Controller {
  private val entries = mutable.HashMap[String, String]()

  post("/set") { req: Entry =>
    entries.put(req.key, req.value)
    req
  }

  get("/get") { req: GetRequest =>
    entries.get(req.key)
      .map(Entry(req.key, _))
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