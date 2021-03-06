import Dependencies._
import scala.sys.process._

def installLmt(baseDir: File, goDir: File) = {
  Process(
    List("go", "get", "-v", "github.com/driusan/lmt"),
    baseDir, 
    "GOPATH" -> goDir.toString
  ) !
}

lazy val goDirectory = settingKey[File]("Subdirectory for Go dependencies (i.e. lmt)")

lazy val lmtExecutable = taskKey[File]("Path to lmt binary. Will install lmt, if necessary.")

lazy val reinstallLmt = taskKey[Unit]("Force an install of lmt.")

lazy val literateSources = settingKey[Seq[File]]("Paths to all Markdown files used to genereate Scala sources.")

lazy val generateSources = taskKey[Unit]("Run lmt to generate Scala source files from literate Markdown sources.")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "akka-websocket",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"   % "10.1.4" ,
      "com.typesafe.akka" %% "akka-stream" % "2.5.12",
      scalaTest % Test
    ),
    goDirectory := baseDirectory.value / "go",
    lmtExecutable := {
      val goDir = goDirectory.value
      val baseDir = baseDirectory.value
      val log = streams.value.log
      val path = goDir / "bin" / "lmt"
      if (!path.exists) {
        log.info("Local lmt executable not found. Installing...")
        installLmt(baseDir, goDir)
      }
      path
    },
    reinstallLmt := {
      installLmt(baseDirectory.value, goDirectory.value)
    },
    literateSources := {
      val blogDir = baseDirectory.value / "blog"
      val appendixDir = blogDir / "appendix"
      Seq(
        blogDir / "03_Our_Server_Scaffold.md",
        appendixDir / "01_CommandAndPushWebSocketHandler_template.md"
      )
    },
    generateSources := {
      val command = Seq(lmtExecutable.value.toString) ++
        literateSources.value.map(_.toString)
    
      command !
    },
    (Compile / compile) := ((Compile / compile) dependsOn generateSources).value
  )
