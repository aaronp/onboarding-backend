import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.OutputPatterns

ThisBuild / organization := "com.github.aaronp"
ThisBuild / name := "onboarding-backend"
ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := "3.4.1"
ThisBuild / scalafmtOnCompile := true
ThisBuild / versionScheme := Some("early-semver")

val LogicFirstVersion = "0.6.0"
val githubResolver = "GitHub Package Registry" at "https://maven.pkg.github.com/kindservices/logic-first"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision


ThisBuild / version := {
  val buildNr = {
    val runNumber = sys.env.getOrElse("GITHUB_RUN_NUMBER", "0").toInt
    // this is my little hack. The run numbers always increase, an we want to reset them when
    // bump to the next version. To do that, we just subtract whatever the last build number was
    // before we incremented the minor version
    runNumber - 0
  }
  val baseVersion = s"0.5.$buildNr"
  if (sys.env.getOrElse("GITHUB_REF", "").contains("refs/heads/main"))
    baseVersion
  else
    s"$baseVersion-SNAPSHOT"
}

addCommandAlias("removeUnusedImports", ";scalafix RemoveUnused")
addCommandAlias("organiseImports", ";scalafix OrganizeImports")

import sbt._
import Keys._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets


// Common settings
lazy val commonSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "kind.onboarding.buildinfo",
  resolvers += githubResolver,
  // ============== UNCOMMENT THIS LINE WHEN YOUR MODELS COME FROM THE SERVICE.YAML ===============
  //
  // this is our model libraries, generated from the service.yaml and created/publised via 'make packageRestCode'
  // you'll need to uncomment this line once you're using data models generated from the service.yaml
  //
  //
  // libraryDependencies += "onboarding" %%% "kind" % "0.0.1",
  // ================================================================================================
  libraryDependencies += "kind" %%% "kind-docstore" % "0.2.0",
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.2.18" % Test
  )
)

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-rewrite",
  "-Xlint",
  "-Wunused:all"
)

lazy val app = crossProject(JSPlatform, JVMPlatform).in(file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(commonSettings).
  jvmSettings(
//    unmanagedJars in Compile ++= unmanagedJVM.toList,
    libraryDependencies ++= Seq(
      "kindservices" %%% "logic-first-jvm" % LogicFirstVersion, // <-- NOTE: this would be better in common settings, but we have a different suffix for jvm and JS

      "com.lihaoyi" %% "cask" % "0.9.2")
  ).
  jsSettings(
    scalaJSUseMainModuleInitializer := false,
    libraryDependencies ++= Seq(
      "kindservices" %%% "logic-first-js" % LogicFirstVersion, // <-- NOTE: this would be better in common settings, but we have a different suffix for jvm and JS
      // "io.github.cquiroz" %%% "scala-java-time" % "2.5.0",
      // "com.lihaoyi" %%% "scalatags" % "0.13.1",
      // "org.scala-js" %%% "scalajs-dom" % "2.4.0"
    ),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
      .withSourceMap(true)
      .withOutputPatterns(OutputPatterns.fromJSFile("%s.mjs")) // see https://www.scala-js.org/doc/project/module.html
    })

lazy val root = project.in(file(".")).
  aggregate(app.js, app.jvm).
  settings(
    publish := {},
    publishLocal := {},
  )


ThisBuild / publishMavenStyle := true

val githubUser = "aaronp"
val githubRepo = "onboarding-backend"
ThisBuild / publishTo := Some("GitHub Package Registry" at s"https://maven.pkg.github.com/$githubUser/$githubRepo")


sys.env.get("ACCESS_TOKEN") match {
  case Some(token) if token.nonEmpty =>

    val actor = sys.env.getOrElse("GITHUB_ACTOR", githubUser)
    ThisBuild / credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      githubUser,
      token
    )
  case _ =>
    println("\n\t\tACCESS_TOKEN not set - assuming a local build\n\n")
    credentials ++= Nil
}


// see https://leonard.io/blog/2017/01/an-in-depth-guide-to-deploying-to-maven-central/
pomIncludeRepository := (_ => false)

// To sync with Maven central, you need to supply the following information:
Global / pomExtra := {
  <url>https://github.com/aaronp/onboarding-backend</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>
          aaronp
        </id>
        <name>Aaron Pritzlaff</name>
        <url>https://github.com/aaronp/onboarding-backend</url>
      </developer>
    </developers>
}
