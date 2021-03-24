ThisBuild / scalaVersion := "2.12.12"

lazy val `key-value-store` = (project in file("."))
  .settings(
      name := "key-value-store",
      libraryDependencies ++= Seq(
        "com.google.inject" % "guice" % "5.0.1",
        "com.twitter" %% "finatra-http" % "21.2.0",
        "org.slf4j" % "slf4j-simple" % "1.7.30",

        // Test deps
        "com.twitter" %% "finatra-jackson" % "21.2.0" % Test classifier "tests",
        "com.twitter" %% "inject-server" % "21.2.0" % Test classifier "tests",
        "com.twitter" %% "inject-app" % "21.2.0" % Test classifier "tests",
        "com.twitter" %% "inject-core" % "21.2.0" % Test classifier "tests",
        "com.twitter" %% "inject-modules" % "21.2.0" % Test classifier "tests",
        "com.twitter" %% "finatra-http" % "21.2.0" % Test classifier "tests",
        "org.scalatest" %% "scalatest" % "3.2.5" % Test,
        "org.scalatestplus" %% "junit-4-13" % "3.2.5.0" % Test,
      ),
)
