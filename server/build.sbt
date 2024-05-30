import scala.collection.Seq

organization := "kind"
name := "docstore-db"
version := "0.0.1"
scalaVersion := "3.4.1"
versionScheme := Some("early-semver")


val LogicFirstVersion = "0.5.3"

val githubResolver = "GitHub Package Registry" at "https://maven.pkg.github.com/kindservices/logic-first"
ThisBuild / resolvers += githubResolver

libraryDependencies += "kind" %% "kind-docstore" % "0.2.0"

libraryDependencies ++= Seq(
  "com.github.aaronp" %% "logic-first-jvm" % LogicFirstVersion,
  "com.lihaoyi" %% "upickle" % "3.0.0",
  "org.mongodb" % "mongodb-driver-sync" % "4.9.1",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-rewrite",
  "-new-syntax",
  "-Wunused:all"
)

addCommandAlias("removeUnusedImports", ";scalafix RemoveUnused")
addCommandAlias("organiseImports", ";scalafix OrganizeImports")

mainClass := Some("kind.docstore.server.Server")

sys.env.get("GITHUB_TOKEN") match {
  case Some(token) if token.nonEmpty =>
    ThisBuild / credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      "aaronp",
      token
    )
  case _ =>
    println("\n\t\tGITHUB_TOKEN not set - assuming a local build\n\n")
    credentials ++= Nil
}

// see https://github.com/sbt/sbt-assembly
assemblyMergeStrategy := {
  case "native-image.properties"                            => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith "native-image.properties" => MergeStrategy.first
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

assembly / test := {}
