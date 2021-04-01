package key_value_store

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.annotations.QueryParam

import java.io.{BufferedWriter, File, FileWriter}
import javax.inject.Inject
import scala.collection.mutable
import scala.io.Source

case class Value(value: String, expirationTimestamp: Option[Long])

case class FileStorage(values: List[SetRequest])

class KeyValueController @Inject() extends Controller {
  private val entries = mutable.HashMap[String, String]()
  private val serializer: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val filePath = "./storage"

//  reload()

//  private def persist(): Unit = {
//    val entriesToStore = entries.toList.map {
//      case (key, value) =>
//      SetRequest(key, value.value, value.expirationTimestamp)
//    }
//
//    val thingToStore = FileStorage(entriesToStore)
//
//    val storageFile = new File(filePath)
//
//    val bw = new BufferedWriter(new FileWriter(storageFile))
//    bw.write(serializer.writeValueAsString(thingToStore))
//    bw.close()
//  }
//
//  private def reload(): Unit = {
//    val storageFile = new File(filePath)
//    if (!storageFile.exists()) {
//      val bw = new BufferedWriter(new FileWriter(storageFile))
//      bw.write(serializer.writeValueAsString(FileStorage(Nil)))
//      bw.close()
//    }
//
//    val source = Source.fromFile(filePath)
//    val json = source.mkString
//
//    val fileStorage = serializer.readValue(json, classOf[FileStorage])
//
//    fileStorage.values.foreach { setReq =>
//      entries.put(setReq.key, Value(setReq.value, setReq.ttl))
//    }
//  }


  // arrays and hashmaps as value
  /*

   calling set "a.b.c" should update the value at { "a": {"b": {"c": "<here>" } } }

   TTL support at innermost level

   <here> is the value
     - can be list
     - can be string
   */

  private def readValue(value: String): Either[String, Map[String, String]] = {
    try {
      val result = serializer.readValue(value, classOf[Map[String, String]])
      Right(result)
    } catch {
      case e: Exception =>
        Left(value)
    }
  }


  post("/set") { req: SetRequest =>
    val ttl = req.ttl.map(ttl => ttl * 1000 + System.currentTimeMillis())


    val parts = req.key.split('.').toList

    val actualKey = parts.head

//    entries.get(actualKey).map { value =>
//      readValue(value)
//    }

    if (parts.length == 1) {
      entries.put(actualKey, req.value)
    } else {
      // assuming length == 2
      val innerKey = parts(1)

      val value = Map(innerKey -> req.value)

      entries.put(actualKey, serializer.writeValueAsString(value))
    }




//    val value = Value(req.value, ttl)
//    entries.put(req.key, value)
//    persist()
    req
  }

  get("/get") { req: GetRequest =>
    val parts = req.key.split('.').toList

    val actualKey = parts.head

    entries.get(actualKey)
//      .filter(v => {
//        val isValid = v.expirationTimestamp.forall(_ > System.currentTimeMillis())
//        // If the timestamp is expired, remove it from the map
//        if (!isValid) {
//          entries.remove(req.key)
////          persist()
//        }
//        isValid
//      })
      .map(v => {
        val value = readValue(v)

        val retVal = {
          if (parts.length == 1) {
            value match {
              case Left(lValue) => lValue
              case Right(rValue) => serializer.writeValueAsString(rValue)
            }
          } else {
            val innerKey = parts(1)
            value match {
              case Left(lValue) =>
                throw new IllegalArgumentException("")
              case Right(rValue) =>
                rValue.getOrElse(innerKey, {
                  throw new IllegalArgumentException("")
                })
            }
          }
        }


        SetRequest(req.key, retVal, None)
      })
      .getOrElse {
        response.notFound(Errors(List(s"Key not found: ${req.key}")))
      }
  }

  post("/delete") { req: DeleteRequestResponse =>
    val resp = entries.remove(req.key)
      .map(_ => {
//        persist()
        req.key
      })
      .getOrElse(s"Key did not exist: ${req.key}")

    DeleteRequestResponse(resp)
  }

//  get("/search") { req: GetRequest =>
//    entries.collectFirst {
//      case (key, value) if key.contains(req.key) =>
//        SetRequest(key, value.value, value.expirationTimestamp)
//    }.getOrElse {
//      response.notFound(Errors(List(s"Key not found: ${req.key}")))
//    }
//  }
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