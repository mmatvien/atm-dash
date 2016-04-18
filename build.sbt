name := "atm"

version := "1.0"

lazy val `atm` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc ,
  cache ,
  ws  ,
  "com.github.jeroenr" %% "tepkin" % "0.7",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  specs2 % Test )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"  