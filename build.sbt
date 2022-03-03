name := "tapir-test"

scalaVersion := "2.12.15"
scalacOptions ++= Seq("-encoding", "utf8", "-Xfatal-warnings", "-deprecation", "-unchecked", "-feature", "-Ypartial-unification", "-Ywarn-unused-import")
javacOptions ++= Seq("-source", "17", "-target", "17")

val akkaVersion           = "2.6.18"
val akkaHttpVersion       = "10.2.7"
val typesafeConfigVersion = "1.4.2"
val scalaTestVersion      = "3.2.9"
val sprayJsonVersion      = "1.3.6" 
val sttpCoreVersion       = "1.4.23"
val tapirVersion          = "0.20.1"

val tapir = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core",
  "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml",
  "com.softwaremill.sttp.tapir" %% "tapir-json-spray"
).map(_ % tapirVersion).map(_ exclude ("com.typesafe.akka", "akka-stream_2.12")).map(_ exclude ("com.typesafe.akka", "akka-http_2.12"))


val testDependencies = Seq(
  "org.scalatest"     %% "scalatest"                % scalaTestVersion,
  "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit"             % akkaVersion,
).map(_ % Test)

val serviceDependencies = Seq(
  "com.typesafe.akka"       %% "akka-http"                       % akkaHttpVersion,
  "com.typesafe.akka"       %% "akka-stream"                     % akkaVersion, // KEEP
  "com.typesafe.akka"       %% "akka-http-spray-json"            % akkaHttpVersion,
  "com.typesafe.akka"       %% "akka-slf4j"                      % akkaVersion,
  "io.spray"                %% "spray-json"                      % sprayJsonVersion,
)

libraryDependencies ++= serviceDependencies ++ testDependencies ++ tapir
