lazy val repoName       = "TravellingAnts"
lazy val projectName    = "travelling-ants"
lazy val projectNameL   = projectName.toLowerCase

lazy val projectVersion = "0.1.1"
lazy val mimaVersion    = "0.1.0"

lazy val root = project.in(file("."))
  .settings(
    name                  := projectName,
    organization          := "de.sciss",
    description           := "Ant colony optimization for travelling salesman problem",
    licenses              := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    homepage              := Some(url(s"https://github.com/Sciss/$repoName")),
    version               := projectVersion,
    mimaPreviousArtifacts := Set("de.sciss" %% projectNameL % mimaVersion),
    scalaVersion          := "2.12.4",
    crossScalaVersions    := Seq("2.12.4", "2.11.12"),
    scalacOptions         ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint")
  )
  .settings(publishSettings)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := { val n = repoName
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>hakuch</id>
    <name>Jesse Haber-Kucharsky</name>
    <url>http://haberkucharsky.com</url>
  </developer>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
</developers>
}
)
