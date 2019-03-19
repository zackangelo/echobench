lazy val commonSettings = Seq(
  organization := "com.bigcommerce",
  scalaVersion := "2.11.8",
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:postfixOps",
    "-encoding", "utf8"
  ),
  assemblyMergeStrategy in assembly := {
    case PathList(xs @ _*) if xs.last == "io.netty.versions.properties" => MergeStrategy.rename
    case PathList("com", "google", "protobuf", xs @ _*) => MergeStrategy.first
    case PathList("com", "google", "api", xs @ _*) => MergeStrategy.first
    case PathList("logback.xml") => MergeStrategy.last
    case PathList("logback-nomad.xml") => MergeStrategy.last
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      if (oldStrategy == MergeStrategy.deduplicate)
        MergeStrategy.first
      else
        oldStrategy(x)
  }
)

lazy val echobench = Project(id = "echobench", base = file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
    "nl.grons" %% "metrics4-scala" % "4.0.1"
  ))
  .settings(logBuffered in Test := false)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    )
  )
  .settings(
    assemblyJarName in assembly := "deadliner.jar",
    mainClass in reStart := Some("com.bigcommerce.deadliner.DeadlinerClient"),
    reStartArgs := Seq(
      "127.0.0.1", //target host
      "4142", //target port
      "100", //num threads
      Integer.MAX_VALUE.toString(), //num requests
      "10" //deadline
    ),
    javaOptions in reStart := Seq(
      "-Djava.util.logging.config.file=logging.properties",
      "-Dlogback.configurationFile=/Users/zack.angelo/git/srvtest/logback.xml"
    )
  )
