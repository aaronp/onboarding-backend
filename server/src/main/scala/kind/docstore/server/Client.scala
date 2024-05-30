package kind.docstore.server

import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.{MongoClients, MongoCollection, MongoDatabase}
import com.mongodb.client.model.Filters
import com.mongodb.client.result.InsertOneResult
import org.bson.Document
import org.bson.conversions.Bson
import upickle.default.*
import ujson.Value
import java.util.Collections
import scala.jdk.CollectionConverters.*
import kind.logic.*

/**
 * A Mongo client
 * @param database the wrapped database
 */
class Client(database: MongoDatabase) {

  def collection(name : String): MongoCollection[Document] = database.getCollection(name)

  def query(path : String, filter: Option[String]): Seq[Value] = {
    collection(path).find().iterator().asScala.toSeq.flatMap { doc =>
      val jason = Option(read[Value](doc.toJson()))
      filter match {
        case Some(text) if jason.get.render(0).contains(text) => jason
        case Some(_) => None
        case None => jason
      }
    }
  }

  def asDoc[A: ReadWriter](id :String, data: A) = Document.parse(data.merge(Map("_id" -> id)).render(0))

  def getById[A: ReadWriter](path : String, id : String) = queryFirst[A](path, Filters.eq("_id", id))

  def insert[A: ReadWriter](path : String, data: A): InsertOneResult = {
    val (name, id) = path.asPathAndId
    collection(name).insertOne(asDoc(id, data))
  }

  def queryFirst[A: ReadWriter](path: String, filter: Bson): Option[A] = {
//    val (name, id) = path.asPathAndId
    Option(collection(path).find(filter).first()).map { retrievedDoc =>
      read[A](retrievedDoc.toJson())
    }
  }

  def findByName[A: ReadWriter](path : String, name: String): Option[A] = queryFirst[A](path, Filters.eq("name", name))
}

object Client {

  /**
   * Note - we know having the password like this is stupid. It's a demo
   *
   */
  case class Config(database : String, user: String, password : String, host: String, port: Int = 27017) {
    def asCredentials = com.mongodb.MongoCredential.createCredential(user, database, password.toCharArray)
    def serverAddress = new ServerAddress(host, port)

  }
  object Config {
    def get(key :String, fallback : Any) = sys.env.get(key).orElse(Option(fallback).map(_.toString)).getOrElse {
      sys.error(s"required environment variable '$key' was not set")
    }
    def forEnv(): Config = {
      new Config(
        database = get("DATABASE", "admin"),
        user = get("MONGO_USER", "root"),
        password = get("MONGO_PASSWORD", "example"),
        host = get("MONGO_HOST", "localhost"),
        port = get("MONGO_PORT", 27017).toInt
      )
    }
  }

  def apply(config : Config = Config.forEnv()) = {
    println(
      s"""
         |
         | Connecting to mongo using:
         | database : ${config.database}
         |     user : ${config.user}
         | password : ${config.password.map(_ => '*')}
         |     host : ${config.host}
         |     port : ${config.port}
         |
         |""".stripMargin)
    val clientSettings = MongoClientSettings.builder()
      .applyToClusterSettings { builder =>
        builder.hosts(Collections.singletonList(config.serverAddress))
      }
      .credential(config.asCredentials)
      .build()

    val client = MongoClients.create(clientSettings)

    val database: MongoDatabase = client.getDatabase(config.database)
    new Client(database)
  }
}

