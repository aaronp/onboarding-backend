import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.OutputPatterns

ThisBuild / organization := "onboarding"
ThisBuild / name := "kind"
ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := "3.4.1"
ThisBuild / scalafmtOnCompile := true
ThisBuild / versionScheme := Some("early-semver")

val LogicFirstVersion = "0.5.1"
val githubResolver = "GitHub Package Registry" at "https://maven.pkg.github.com/kindservices/logic-first"
ThisBuild / resolvers += githubResolver

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

addCommandAlias("removeUnusedImports", ";scalafix RemoveUnused")
addCommandAlias("organiseImports", ";scalafix OrganizeImports")

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
    libraryDependencies ++= Seq(
      "com.github.aaronp" %%% "logic-first-jvm" % LogicFirstVersion, // <-- NOTE: this would be better in common settings, but we have a different suffix for jvm and JS
      "com.lihaoyi" %% "cask" % "0.9.2")
  ).
  jsSettings(
    scalaJSUseMainModuleInitializer := false,
    libraryDependencies ++= Seq(
      "com.github.aaronp" %%% "logic-first-js" % LogicFirstVersion, // <-- NOTE: this would be better in common settings, but we have a different suffix for jvm and JS
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
val githubRepo = "kind"
ThisBuild / publishTo := Some("GitHub Package Registry" at s"https://maven.pkg.github.com/$githubUser/$githubRepo")

sys.env.get("GITHUB_TOKEN") match {
  case Some(token) if token.nonEmpty =>
    ThisBuild / credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      githubUser,
      token
    )
  case _ =>
    println("\n\t\tGITHUB_TOKEN not set - assuming a local build\n\n")
    credentials ++= Nil
}