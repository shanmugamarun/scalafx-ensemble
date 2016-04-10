import java.io.File

name := "ScalaFX Ensemble"

version := "1.0.1"

organization := "org.scalafx"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.72-R10-SNAPSHOT",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
)

resolvers += Opts.resolver.sonatypeSnapshots

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

// Sources should also be copied to output, so the sample code, for the viewer,
// can be loaded from the same file that is used to execute the example
unmanagedResourceDirectories in Compile <+= baseDirectory {_ / "src/main/scala"}

// Set the prompt (for this build) to include the project id.
shellPrompt := { state => System.getProperty("user.name") + ":" + Project.extract(state).currentRef.project + "> " }

// Run in separate VM, so there are no issues with double initialization of JavaFX
fork := true

fork in Test := true

// Create file used to determine available examples at runtime.
resourceGenerators in Compile <+= Def.task {
  /** Scan source directory for available examples
    * Return pairs 'directory' -> 'collectionof examples in that directory'.
    */
  def loadExampleNames(inSourceDir: File): Array[(String, Array[String])] = {
    val examplesDir = "/scalafx/ensemble/example/"
    val examplePath = new File(inSourceDir, examplesDir)
    val exampleRootFiles = examplePath.listFiles()
    for (dir <- exampleRootFiles if dir.isDirectory) yield {
      val leaves = for (f <- dir.listFiles() if f.getName.contains(".scala")) yield {
        f.getName.stripSuffix(".scala").stripPrefix("Ensemble")
      }
      dir.getName.capitalize -> leaves.sorted
    }
  }

  /** Create file representing names and directories for all availabe examples.
    * It will be loaded by the application at runtime and used to popolate example tree.
    */
  def generateExampleTreeFile(inSourceDir: File,
                              outSourceDir: File,
                              templatePath: String): Seq[File] = {
    val exampleDirs = loadExampleNames(inSourceDir)
    val contents = exampleDirs.map { case (dir, leaves) => dir + " -> " + leaves.mkString(", ") }.mkString("\n")
    val outFile = new File(outSourceDir, templatePath)
    IO.write(outFile, contents)

    Seq(outFile)
  }

  generateExampleTreeFile(
    (scalaSource in Compile).value,
    (resourceManaged in Compile).value,
    "/scalafx/ensemble/example/example.tree"
  )
}

mainClass in Compile := Some("scalafx.ensemble.Ensemble")
mainClass in assembly := Some("scalafx.ensemble.Ensemble")

//
// Configuration for sbt-native-packager / JDKPackagerPlugin
//

enablePlugins(JDKPackagerPlugin)

maintainer := "ScalaFX Organization (scalafx.org)"
packageSummary := "Collection of live ScalaFX examples"
packageDescription := "An application demonstrating ScalaFX code samples."

lazy val iconGlob = sys.props("os.name").toLowerCase match {
  case os if os.contains("mac") => "*.icns"
  case os if os.contains("win") => "*.ico"
  case _ => "*.png"
}

jdkAppIcon := (sourceDirectory.value ** iconGlob).getPaths.headOption.map(file)
jdkPackagerType := "installer"

// this is to help ubuntu 15.10
antPackagerTasks in JDKPackager := (antPackagerTasks in JDKPackager).value orElse {
  for {
    f <- Some(file("/usr/lib/jvm/java-8-oracle/lib/ant-javafx.jar")) if f.exists()
  } yield f
}
