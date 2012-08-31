package eu.ehri.project.test

import eu.ehri.project.core.GraphHelpers

import eu.ehri.project.models._
import eu.ehri.project.relationships._
import com.tinkerpop.frames.FramedGraph
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.blueprints.{ Vertex, Edge }
import scala.collection.JavaConversions._
import com.sun.corba.se.impl.protocol.IsA

object DataLoader {
  val DESCRIPTOR = "DESCRIPTOR" // Hack indexed attribute for finding test nodes
}

class DataLoader(val graph: FramedGraph[Neo4jGraph]) {
  val helper = new GraphHelpers(graph.getBaseGraph.getRawGraph)

  def createRelationship(src: Vertex, dst: Vertex, label: String, data: Map[String, Any] = Map()) = {
    helper.getOrCreateIndex(label, classOf[Edge])
    helper.createIndexedEdge(
      src.getId().asInstanceOf[java.lang.Long],
      dst.getId().asInstanceOf[java.lang.Long],
      label, data.asInstanceOf[Map[String, Object]], label)
  }

  /*
   * Shortcut for creating a relationship from test descriptors
   */
  def CR(src: String, dst: String, label: String, data: Map[String, Any] = Map()) = {
    try {
      createRelationship(findTestElement(src), findTestElement(dst), label, data)
    } catch {
      case e: NoSuchElementException => throw new NoSuchElementException("For items: %s or %s".format(src, dst))
    }
  }

  /*
   * Find a raw vertex with the given descriptor
   */
  def findTestElement(descriptor: String): Vertex = try {
    graph.getBaseGraph.getVertices(DataLoader.DESCRIPTOR, descriptor).head
  } catch {
    case e: NoSuchElementException => throw new NoSuchElementException(
      "Error finding item with descriptor: '%s'".format(descriptor))
  }

  /*
   * Find a specific Framed type `T` with the given descriptor.
   */
  def findTestElement[T](descriptor: String, cls: Class[T]): T = try {
    graph.getVertices(DataLoader.DESCRIPTOR, descriptor, cls).head
  } catch {
    case e: NoSuchElementException => throw new NoSuchElementException(
      "Error finding item with descriptor: '%s'".format(descriptor))
  }

  /*
   * Generic vertices with a given isA type
   */
  def findTestElements(isa: String) = {
    graph.getBaseGraph.getVertices("isA", isa).toList
  }

  /*
   * Get `T` Frames vertices with a given isA type
   */
  def findTestElements[T](isa: String, cls: Class[T]) = {
    graph.getVertices("isA", isa, cls).toList
  }

  def loadTestData = {

    val tx = graph.getBaseGraph.getRawGraph.beginTx

    // Create all the nodes
    TestData.nodes.foreach {
      case TestData.NN(desc, node) =>
        node.get("isA").map {
          case (index: String) =>
            helper.getOrCreateIndex(index, classOf[Vertex])
            helper.createIndexedVertex(
              (node + (DataLoader.DESCRIPTOR -> desc)).asInstanceOf[Map[String, Object]], index)
        }
    }

    // Load edges using node descriptors to find source and target
    TestData.edges.foreach {
      case TestData.R(src, label, dst, data) =>
        CR(src, dst, label, data)
    }

    tx.success()
  }
}
