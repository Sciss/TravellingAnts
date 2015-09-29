lazy val root = (project in file("."))
  .settings(
    name := "travelling-ants",
    version := "0.1.0",
    scalaVersion := "2.11.7",

    libraryDependencies += "org.scalaz" %% "scalaz-concurrent" % "7.1.4"
)

enablePlugins(JavaAppPackaging)

// Since we print the result to stdout, suppress sbt's own output.
showSuccess := false
