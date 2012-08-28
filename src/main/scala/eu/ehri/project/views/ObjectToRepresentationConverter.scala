package eu.ehri.project.views

import eu.ehri.project.models._

import com.tinkerpop.frames.VertexFrame
import com.tinkerpop.frames.Adjacency
import com.tinkerpop.blueprints.Vertex
import eu.ehri.project.models.annotations
import java.lang.annotation.Annotation
import java.lang.reflect.Method
import org.apache.commons.collections.map.MultiValueMap
import eu.ehri.project.persistance.EntityBundle
import eu.ehri.project.exceptions.SerializationError  

import scala.collection.JavaConversions._

import com.codahale.jerkson.Json

object ObjectToRepresentationConverter extends DataConverter {

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

  def serialize(item: VertexFrame): Map[String, Any] = {
    var cls = item.getClass.getInterfaces.toList.head
    var relations = Map[String, Any]()
    // Traverse the methods of the item's class, looking for
    // @Adjacency annotations also annotated with @Fetch
    for (method <- cls.getMethods()) {
      if (isFetchMethod(method)) {
        getAdjacencyLabel(method).map { label =>
          method.setAccessible(true);
          method.invoke(item) match {
            case iter: java.lang.Iterable[VertexFrame] => {
              relations = relations + (label -> iter.toList.map(serialize))
            }
            case single: VertexFrame => {
              relations = relations + (label -> serialize(single))
            }
            case _ => // don't know how to serialize this, so just skip it!
          }
        }
      }
    }
    Map(
      "id" -> item.asVertex().getId(),
      "data" -> vertexData(item.asVertex),
      "relationships" -> relations)
  }

  def bundleToData[T <: VertexFrame](bundle: EntityBundle[T]): Map[String,Any] = {
    var relations = Map[String,List[Any]]()
    for (key <- bundle.getSaveWith().asInstanceOf[MultiValueMap].keySet()) {
      val c = bundle.getSaveWith.getCollection(key).asInstanceOf[java.util.Collection[EntityBundle[T]]]
      relations = relations + (key.asInstanceOf[String] -> c.toList.map(bundleToData[T]))      
    }
    
    Map(
      "id" -> bundle.getId(),
      "data" -> bundle.getData(),
      "relationships" -> relations
    )
  }
    
  def vertexFrameToBundle[T <: VertexFrame](item: VertexFrame): EntityBundle[T] = {
    val isa = item.asVertex().getProperty(EntityTypes.KEY).asInstanceOf[String]
    val cls = classes.getOrElse(isa, 
            throw new SerializationError("No isa found for vertex: %s".format(item)))
    
    var relations = Map[String, List[EntityBundle[T]]]()
    // Traverse the methods of the item's class, looking for
    // @Adjacency annotations also annotated with @Fetch
    for (method <- cls.getMethods()) {
      if (isFetchMethod(method)) {
        getAdjacencyLabel(method).map { label =>
          method.setAccessible(true);
          method.invoke(item) match {
            case iter: java.lang.Iterable[VertexFrame] => {
              relations = relations + (label -> iter.toList.map(vertexFrameToBundle[T]))
            }
            case single: T => {
              relations = relations + (label -> List(vertexFrameToBundle[T](single)))
            }
            case _ => // don't know how to serialize this, so just skip it!
          }
        }
      }
    }
    val bundle = new EntityBundle[T](
        item.asVertex.getId().asInstanceOf[Long],
        vertexData(item.asVertex), cls.asInstanceOf[Class[T]], new MultiValueMap)    
    relations.foldLeft(bundle) { case (bd, (rel, rels)) =>
      rels.foldLeft(bd) { case (bd, r) =>
        bd.saveWith(rel, r)        
      }
    }
  }

  
  def convert(map: Map[String, Any]): String = Json.generate(map)
  def convert[T <: VertexFrame](bundle: EntityBundle[T]): String = convert(bundleToData(bundle))
  def convert(item: VertexFrame): String = convert(bundleToData(vertexFrameToBundle(item)))
}