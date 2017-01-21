// sbt-scalariform
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.scalariformSettingsWithIt
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

// sbt-release
import sbtrelease._
import sbtrelease.Utilities._
import ReleaseStateTransformations._

// Resolvers
resolvers ++= Seq(
  "ctek repo" at "https://century.artifactoryonline.com/century/libs-release/"
)
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// Dependencies
val testDependencies = Seq (
  "com.typesafe.akka"          %% "akka-testkit"                   % "2.4.6"  % "test,it",
  "com.typesafe.akka"          %% "akka-http-testkit"              % "2.4.6"  % "test,it",
  "org.scalacheck"             %% "scalacheck"                     % "1.12.5" % "test,it",
  "org.specs2"                 %% "specs2-core"                    % "3.6.6"  % "test,it",
  "org.specs2"                 %% "specs2-mock"                    % "3.6.6"  % "test,it",
  "org.specs2"                 %% "specs2-matcher-extra"           % "3.6.6"  % "test,it"
)

val rootDependencies = Seq(
  "ch.qos.logback"             %  "logback-classic"              % "1.1.3",
  "net.logstash.logback"       %  "logstash-logback-encoder"     % "4.6",
  "com.github.nscala-time"     %% "nscala-time"                  % "2.8.0",
  "com.typesafe"               %  "config"                       % "1.3.0",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j"          % "2.1.2",
  "com.typesafe.akka"          %% "akka-actor"                   % "2.4.6",
  "com.typesafe.akka"          %% "akka-slf4j"                   % "2.4.6",
  "com.typesafe.akka"          %% "akka-http-experimental"       % "2.4.6",
  "de.heikoseeberger"          %% "akka-http-argonaut"           % "1.6.0",
  "io.argonaut"                %% "argonaut"                     % "6.1",
  "net.ceedubs"                %% "ficus"                        % "1.1.2",
  "commons-daemon"             %  "commons-daemon"               % "1.0.15",
  "com.datastax.cassandra"     %  "cassandra-driver-core"        % "2.1.9",
  "javassist"                  %  "javassist"                    % "3.12.1.GA", // for cassandra driver
  "com.google.code.findbugs"   %  "jsr305"                       % "3.0.1" % "provided", // for datastax driver compiler warning
  "org.mongodb.scala"          %% "mongo-scala-bson"             % "1.1.0",
  "org.spire-math"             %% "cats"                         % "0.3.0",
  "joda-time"                  % "joda-time"                     % "2.9.4",
  "io.ctek.common"             %% "common-akka-helpers"          % "0.1.2"
)

al dependencies =
  rootDependencies ++
   testDependencies

// Settings
//
val forkedJvmOption = Seq(
  "-server",
  "-Dfile.encoding=UTF8",
  "-Duser.timezone=GMT",
  "-Xss1m",
  "-Xms1048m",
  "-Xmx1048m",
  "-XX:+CMSClassUnloadingEnabled",
  "-XX:+DoEscapeAnalysis",
  "-XX:+UseConcMarkSweepGC",
  "-XX:+UseParNewGC",
  "-XX:+UseCodeCacheFlushing",
  "-XX:+UseCompressedOops"
)

val buildSettings = Seq(
  name := "abacus",
  organization := "io.ctek.services",
  scalaVersion := "2.11.8",
  scalaBinaryVersion := "2.11"
)

val compileSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:_",
    "-unchecked",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )
)

val formatting =
  FormattingPreferences()
    .setPreference(AlignParameters, false)
    .setPreference(AlignSingleLineCaseStatements, false)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
    .setPreference(CompactControlReadability, false)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(FormatXml, true)
    .setPreference(IndentLocalDefs, false)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentWithTabs, false)
    .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
    .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
    .setPreference(PreserveSpaceBeforeArguments, false)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(SpacesAroundMultiImports, false)
    .setPreference(SpacesWithinPatternBinders, true)

// release settings
val mainProjectRef = LocalProject("main")

val releaseDirectory = TaskKey[File]("release-directory")

val emitJenkinsVersionFiles: ReleaseStep = { state: State =>
  val v = state.extract.get(version in ThisBuild)
  state.log.info("Writing jenkins version files ...")
  IO.write(new File("version.jenkins"), s"RELEASE_VERSION=$v")
  IO.write(new File("package.json"), s"""{"version":"$v"}""")
  state
}

val loginAWSDockerRegistry: ReleaseStep = { state: State =>
  state.log.info("Login into AWS Docker Registry ...")
  val dockerCommand = "/usr/local/bin/aws --region us-east-1 ecr get-login".!!
  dockerCommand.!
  state
}

val releaseSettings = Seq(
  releaseProcess := Seq[ReleaseStep](
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    loginAWSDockerRegistry,
    ReleaseStep(releaseStepTask(publish in Docker)),
    emitJenkinsVersionFiles,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

val dockerSettings = Seq(
  maintainer in Docker := "Century Tech <root@ctek.io>",
  daemonUser in Docker := "root",
  dockerRepository := Option("609207979550.dkr.ecr.us-east-1.amazonaws.com/analytics"),
  dockerBaseImage := "anapsix/alpine-java:jre8",
  dockerExposedPorts := Seq(9000),
  dockerExposedVolumes := Seq("/opt/docker/logs", "/opt/docker/etc"),
  javaOptions in Universal ++= Seq(
    "-Dconfig.file=./etc/abacus.conf",
    "-Dlogback.configurationFile=./etc/abacus_logback.xml"
  )
)

val pluginsSettings =
  compileSettings ++
  buildSettings ++
  releaseSettings ++
  dockerSettings ++
  scalariformSettingsWithIt

val settings = Seq(
  libraryDependencies ++= dependencies,
  fork in run := true,
  fork in Test := true,
  fork in testOnly := true,
  connectInput in run := true,
  javaOptions in run ++= Seq("-Dconfig.file=./etc/abacus.conf", "-Dlogback.configurationFile=./etc/abacus_logback.xml") ++ forkedJvmOption,
  javaOptions in Test ++= forkedJvmOption,
  mainClass in Compile := Option("io.ctek.services.abacus.AbacusRunner"),
  mainClass in run := Option("io.ctek.services.abacus.AbacusRunnerConsole"),
  // build info
  //
  buildInfoKeys := Seq[BuildInfoKey](name, version),
  buildInfoPackage := "io.ctek.services.abacus.info",
  // release
  //
  releaseDirectory <<= baseDirectory map ( base => base / "release"),
  publishArtifact in (Compile, packageBin) := false,
  // formatting
  //
  ScalariformKeys.preferences := formatting,
  // this makes provided dependencies work when running and uses the correct main class
  //
  run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (run), runner in (Compile, run))
)

lazy val main =
  project
    .in(file("."))
    .enablePlugins(BuildInfoPlugin)
    .enablePlugins(JavaAppPackaging)
    .configs(IntegrationTest)
    .settings(
      pluginsSettings ++
      Defaults.itSettings ++
      settings:_*
    )
    .settings(
      compile in Compile <<= (compile in Compile)
    )
