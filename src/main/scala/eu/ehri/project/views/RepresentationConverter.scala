package eu.ehri.project.views

import java.lang.reflect.Method

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.apache.commons.collections.map.MultiValueMap

import com.codahale.jerkson.Json
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.frames.Adjacency
import com.tinkerpop.frames.VertexFrame

import eu.ehri.project.core.utils.AnnotationUtils
import eu.ehri.project.exceptions.DeserializationError
import eu.ehri.project.exceptions.SerializationError
import eu.ehri.project.models.annotations.EntityType
import eu.ehri.project.models.EntityTypes
import eu.ehri.project.models.annotations
import eu.ehri.project.persistance.BundleFactory
import eu.ehri.project.persistance.EntityBundle
import net.liftweb.json.JsonAST.JArray
import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json._

/**
 * Helper class for extracting data from a JSON bundle.
 */
case class InsertBundle(val id: Option[Long], val data: JObject, val relationships: Map[String, List[JValue]])

class RepresentationConverter extends DataConverter {
  
  type JMap = java.util.Map[String, Object]
  type JHMap = java.util.HashMap[String, Object]

  // Implicit necessary for lift-json to do it's work.
  implicit val formats = Serialization.formats(NoTypeHints)

  /**
   * Get the label of a method's Ajacency annotation, or None.
   */
  private def getAdjacencyLabel(method: Method) = method.getAnnotation(classOf[Adjacency]) match {
    case null => None
    case a => Some(a.label());
  }

  /**
   * Convert a vertex into a Map.
   */
  private def vertexData(item: Vertex) = item.getPropertyKeys.map { k =>
    (k -> item.getProperty(k))
  }.toMap

  /**
   * Convert a JSON string to an EntityUpdateBundle of type `T`.
   */
  def convert[T <: VertexFrame](str: String): EntityBundle[T] = jsonToBundle[T](parse(str))

  def convert(map: java.util.Map[String, Object]): String = Json.generate(map)

  def convert[T <: VertexFrame](bundle: EntityBundle[T]): String = convert(bundleToData(bundle))

  def convert(item: VertexFrame): String = convert(bundleToData(vertexFrameToBundle(item)))

  /**
   * Convert a set of nested Maps into a bundle of type `T`.
   */
  def dataToBundle[T <: VertexFrame](jdata: JMap): EntityBundle[T] = {
    // Apologies for this mess. It's complicated by the need to convert
    // back and forth between Scala and Java data types, though I remain
    // convinced that writing it in Java would be too painful.
    try {
      val data = jdata.asScala
      val id = jdata.get("id")
      val props = data.get("data").map(_.asInstanceOf[JMap].asScala).getOrElse(
        throw new DeserializationError("No item data map found"))
      val isa: String = props.get(EntityType.KEY).map(_.asInstanceOf[String]).getOrElse(
        throw new DeserializationError("No '%s' attribute found in item data".format(EntityType.KEY)))
      val cls = classes.get(isa).map(_.asInstanceOf[Class[T]]).getOrElse(
        throw new DeserializationError("No class found for type %s type: '%s'".format(EntityType.KEY, isa)))
      val relations: JMap = data.get("relationships").map(_.asInstanceOf[JHMap]).getOrElse(new JHMap()) // Also Optional!

      val m = new MultiValueMap()
      for (entry <- relations.entrySet) {
        for (item <- entry.getValue.asInstanceOf[java.util.List[JHMap]]) {
          m.put(entry.getKey, dataToBundle[T](item));
        }
      }

      new EntityBundle[T](if (id != null) id.asInstanceOf[Long] else null, props.asJava, cls, m)
    } catch {
      // We're highly liable to ClassCastExceptions here, in which case it must
      // be a problem with the underlying data. Bail out and throw a deserialisation
      // error with the cause.
      case e: ClassCastException => throw new DeserializationError("Error deserializing data", e)
    }
  }

  /**
   * De-serialize a Json value to an an EntityUpdateBundle of type `T`.
   * 
   * TODO: Although it might be more efficient deserializing a bundle
   * directly from JSON, it'd probably be nicer to do it via generic
   * Map data via chained converters.
   */
  def jsonToBundle[T <: VertexFrame](data: JValue): EntityBundle[T] = {
    val ext = data.extract[InsertBundle]

    // we must have an isA to tell us the class to instantiate this to.
    val isA: String = ext.data.values.get(EntityType.KEY).map(_.asInstanceOf[String]).getOrElse(
      throw new DeserializationError("Object has no 'ISA' field"))
    val cls = classes.getOrElse(isA,
      throw new DeserializationError(
        "ISA type '%s' is not a valid entity type %s".format(isA, classes))).asInstanceOf[Class[T]]

    val bf = new BundleFactory[T]
    val bundle = ext.id.map(id =>
      bf.buildBundle(id, ext.data.values.asInstanceOf[Map[String, Object]], cls)).getOrElse(
      bf.buildBundle(ext.data.values.asInstanceOf[Map[String, Object]], cls))

    val deps = getDependentRelations(bundle.getBundleClass)
    ext.relationships.filterKeys(r => deps.contains(r)).foldLeft(bundle) {
      case (bd, (reltype, list)) => list.foldLeft(bd) { case (bd, (cdata: JValue)) =>
        bd.addRelation(reltype, jsonToBundle[T](cdata))
      }
    }
  }

  /**
   * Convert a bundle into a set of nested Maps.
   */
  def bundleToData[T <: VertexFrame](bundle: EntityBundle[T]): java.util.Map[String, Object] = {
    var relations = Map[String, List[Any]]()
    for (key <- bundle.getRelations.asInstanceOf[MultiValueMap].keySet()) {
      val c = bundle.getRelations.getCollection(key).asInstanceOf[java.util.Collection[EntityBundle[T]]]
      relations = relations + (key.asInstanceOf[String] -> c.toList.map(bundleToData[T]))
    }

    Map(
      "id" -> bundle.getId(),
      "data" -> bundle.getData(),
      "relationships" -> relations)
  }

  /**
   * Convert a VertexFrame[T] into a bundle[T].
   */
  def vertexFrameToBundle[T <: VertexFrame](item: VertexFrame): EntityBundle[T] = {
    val isa = item.asVertex().getProperty(EntityType.KEY).asInstanceOf[String]
    val cls = classes.getOrElse(isa,
      throw new SerializationError("No isa found for vertex: %s".format(item)))

    var relations = Map[String, List[EntityBundle[T]]]()
    // Traverse the methods of the item's class, looking for
    // @Adjacency annotated with @Fetch
    for (method <- cls.getMethods()) {
      if (AnnotationUtils.isFetchMethod(method)) {
        getAdjacencyLabel(method).map { label =>
          method.setAccessible(true);
          method.invoke(item) match {
            case iter: java.lang.Iterable[T] => {
              relations = relations + (label -> iter.toList.map(vertexFrameToBundle[T]))
            }
            case single: VertexFrame => {
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
    relations.foldLeft(bundle) {
      case (bd, (rel, rels)) =>
        rels.foldLeft(bd) {
          case (bd, r) =>
            bd.addRelation(rel, r)
        }
    }
  }
}