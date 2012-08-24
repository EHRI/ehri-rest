name := "ehri-data-frames"

organization := "EHRI"

version := "0.1-SNAPSHOT"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
    "org.specs2" %% "specs2" % "1.6.1",
    "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test",
    "com.tinkerpop" % "frames" % "2.1.0",
    "com.tinkerpop.gremlin" % "gremlin-java" % "2.1.0",
    "com.tinkerpop.blueprints" % "blueprints-neo4j-graph" % "2.1.0",
    "org.neo4j" % "neo4j" % "1.8.M07",
    "org.scalaj" %% "scalaj-collection" % "1.2",
    "org.neo4j" % "neo4j-kernel" % "1.8.M07",
    "org.neo4j" % "neo4j-kernel" % "1.8.M07" % "test" classifier "tests",
    "com.codahale" %% "jerkson" % "0.5.0",
    "commons-collections" % "commons-collections" % "3.2.1"
)

resolvers ++= Seq(
    "Neo4j Maven 2 release repository" at "http://m2.neo4j.org/releases",
    "Codahale Jerkson" at "http://repo.codahale.com"
)

