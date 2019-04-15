name := "milo-client-examples"
version := "1.0"
scalaVersion := "2.12.7"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"


libraryDependencies ++= Seq(
  "org.eclipse.milo" % "sdk-client" % "0.3.0-SNAPSHOT",
  "org.eclipse.milo" % "server-examples" % "0.3.0-SNAPSHOT",
  "org.eclipse.milo" % "opc-ua-stack" % "0.3.0-SNAPSHOT",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
  "org.scala-lang.modules" %% "scala-async" % "0.10.0"
)
