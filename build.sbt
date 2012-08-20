name := "ehri-data-frames"

organization := "EHRI"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
    "org.specs2" %% "specs2" % "1.6.1",
    "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test",
    "com.tinkerpop" % "frames" % "2.1.0",
    "org.neo4j" % "neo4j" % "1.8.M07",
    "org.neo4j" % "neo4j-kernel" % "1.8.M07",
    "org.neo4j" % "neo4j-kernel" % "1.8.M07" % "test" classifier "tests"
)

resolvers += "Neo4j Maven 2 release repository" at "http://m2.neo4j.org/releases"

