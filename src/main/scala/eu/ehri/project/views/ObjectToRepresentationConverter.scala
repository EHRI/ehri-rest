package eu.ehri.project.views

import eu.ehri.project.models._

import com.tinkerpop.frames.VertexFrame
import com.tinkerpop.frames.Adjacency
import com.tinkerpop.blueprints.Vertex
import eu.ehri.project.models.annotations
import java.lang.annotation.Annotation
import java.lang.reflect.Method

import scala.collection.JavaConversions._

import com.codahale.jerkson.Json

object ObjectToRepresentationConverter {

  private def isFetchMethod(method: Method): Boolean = method.getAnnotation(classOf[annotations.Fetch]) match {
    case null => false
    case a => true
  }

  private def getAdjacencyLabel(method: Method) = method.getAnnotation(classOf[Adjacency]) match {
    case null => None
    case a => Some(a.label());
  }

  private def vertexData(item: Vertex) = item.getPropertyKeys.map { k =>
    (k -> item.getProperty(k))
  }.toMap

  def serialize(v: AnyRef): Map[String, Any] = v match {
    case item: VertexFrame => {

      var cls = item.getClass.getInterfaces.toList.head
      var relations = Map[String, Any]()
      // Traverse the methods of the item's class, looking for
      // @Adjacency annotations also annotated with @Fetch
      for (method <- cls.getMethods()) {
        if (isFetchMethod(method)) {
          getAdjacencyLabel(method).map { rel =>
            method.setAccessible(true);
            method.invoke(item) match {
              case iter: java.lang.Iterable[VertexFrame] => {
                relations = relations + (rel -> iter.toList.map(serialize))
              }
              case single: VertexFrame => {
                relations = relations + (rel -> serialize(single))
              }
              case _ => // don't know how to serialize this!
            }
          }
        }
      }
      Map(
        "id" -> item.asVertex().getId(),
        "data" -> vertexData(item.asVertex),
        "relationships" -> relations)
    }
    case _ => Map()
  }

  def convert(map: Map[String, Any]): String = Json.generate(map)

  def convert(item: VertexFrame): String = convert(serialize(item))
}