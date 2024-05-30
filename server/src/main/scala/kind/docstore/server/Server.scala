//> using scala "3.4.1"
//> using lib "kind::kind-docstore:0.2.0"
//> using repositories https://maven.pkg.github.com/aaronp/onboarding-backend
package kind.docstore.server

import com.mongodb.client.result.InsertOneResult
import upickle.default.*
import ujson.Value

import scala.jdk.CollectionConverters.*
import kind.logic.*
import org.bson.Document

/**
* This single file can contain the business logic for a REST service.
*
* ```sh
* docker build . -t kind-docstore:latest
* ```
 *
* ======================
* == Building Locally ==
* ======================
* This project can be built using [[scala-clit][https://scala-cli.virtuslab.org]]
*
* To simply run the project
* ```sh
* scala-cli Server.scala
* ```
*
* To create a runnable jar, run:
* ```sh
* scala-cli --power package Server.scala -o app-assembly --assembly
* ```
*
* To produce a docker image (no need for the Dockerfile), run:
* ```sh
* scala-cli --power package --docker Server.scala --docker-image-repository app-docker
* ```
*
*/

import kind.onboarding.docstore.BaseApp
import kind.onboarding.docstore.api.*
import kind.onboarding.docstore.model.*
import scala.util.*
import java.io.File
import kind.logic.*
import kind.logic.json.*

/**
 * Note: We can write all of this in terms of PathTree saved against a single collection
 * as a first-pass for a functionally complete (though horribly unperformant) implementation
 */
case class ClientDb(client : Client) extends DefaultService {
  def compareDocuments(compareDocumentsRequest: CompareDocumentsRequest): CompareDocuments200Response | CompareDocuments400Response = {
    println(s"compareDocuments $compareDocumentsRequest")
    ???
  }

  def copyDocument(copyDocumentRequest: CopyDocumentRequest): CopyDocument200Response | CopyDocument404Response = {
    println(s"copyDocument $copyDocumentRequest")
    ???
  }


  def deleteDocument(path: String): DeleteDocument200Response | GetDocument404Response = {
    println(s"deleteDocument $path")
    ???
  }

  private def getById(path: String): Option[Json] = {
    val (name, id) = path.asPathAndId
    client.getById[Value](name, id)
  }

  def getDocument(path: String, version: Option[String]): Value | GetDocument404Response = {
     getById(path) match {
      case None => GetDocument404Response(Option(s"Couldn't find $path"))
      case Some(value) => value
    }
  }

  def getMetadata(path: String): GetMetadata200Response = {
    println(s"getMetadata $path")
    ???
  }


  def query(path: String, filter: Option[String]): List[Value] = {
    println(s"query $path")
    // note = this is stupid. don't do this
    client.query(path, filter).toList
  }

  def saveDocument(path: String, body: Value): SaveDocument200Response = {
    println(s"saveDocument $path")
    Try(client.insert[Value](path, body)) match {
      case Success(response) =>
        SaveDocument200Response(Some(response.getInsertedId.asObjectId().getValue.toHexString))
      case Failure(err) =>
        println(s"Failed to save $path: $body --> $err")
        SaveDocument200Response(None)
    }
  }

  def updateDocument(path: String, body: Value): UpdateDocument200Response | GetDocument404Response = {
    println(s"updateDocument $path")
    val jason = getById(path) match {
      case Some(existing) => existing.merge(body)
      case None => body
    }
    UpdateDocument200Response {
      saveDocument(path, jason).message
    }
  }

  override def listChildren(path: String): List[String] = {
    println(s"listChildren $path")
    Nil
  }
}

/** This is your main entry point for your REST service
 *  It extends BaseApp which defines the business logic for your services
 */
object Server extends BaseApp(appDefaultService = ClientDb(Client())):
  start()

