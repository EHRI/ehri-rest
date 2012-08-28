package eu.ehri.project.views

import com.tinkerpop.frames._
import scala.collection.JavaConversions._
import eu.ehri.project.persistance._
import eu.ehri.project.core.utils.ClassUtils
import eu.ehri.project.models.annotations.{ EntityType, Dependent }
import eu.ehri.project.models.EntityTypes
import com.codahale.jerkson.Json
import net.liftweb.json._
import net.liftweb.json.JsonAST.{ JValue, JObject, JArray }
import eu.ehri.project.exceptions.DeserializationError


case class InsertBundle(val id: Option[Long], val data: JObject, val relationships: Map[String, JValue])

object RepresentationToObjectConverter {
  // Implicit necessary for lift-json to do it's work.
  implicit val formats = Serialization.formats(NoTypeHints)

  /**
   * Convenience typedef of a class that is a subtype of VertexFrame
   */
  private type EntityClass = Class[_ <: VertexFrame]  
  
  /**
   * Lazy-loaded lookup of 
   */
  private lazy val classes: Map[String, EntityClass] = getEntityClasses

  
  /**
   * Convert a JSON string to an EntityUpdateBundle of type `T`.
   */
  def convert[T <: VertexFrame](str: String) = deserialize[T](parse(str))
  
  /**
   * De-serialize a Json value to an an EntityUpdateBundle of type `T`.
   */
  def deserialize[T <: VertexFrame](data: JValue): EntityBundle[T] = {
    val ext = data.extract[InsertBundle]

    // we must have an isA to tell us the class to instantiate this to.
    val isA: String = ext.data.values.getOrElse(EntityTypes.KEY,
                throw new DeserializationError("Object has no 'ISA' field")).asInstanceOf[String]
    val cls = classes.getOrElse(isA,
                throw new DeserializationError(
                    "ISA type '%s' is not a valid entity type %s".format(isA, classes))).asInstanceOf[Class[T]]
     
    val bf = new BundleFactory[T]
    val bundle = ext.id.map( id =>
        bf.buildBundle(id, ext.data.values.asInstanceOf[Map[String, Object]], cls)).getOrElse(
        bf.buildBundle(ext.data.values.asInstanceOf[Map[String, Object]], cls))
                
    val deps = getDependentRelations(bundle.getBundleClass)
    ext.relationships.filterKeys(r => deps.contains(r)).foldLeft(bundle) {
      case (bd, (reltype, data)) =>
        data match {
          case JArray(list) => list.foldLeft(bd) {
            case (bd, (cdata: JValue)) =>
              bd.saveWith(reltype, deserialize[T](cdata))
          }
          case single: JValue => bd.saveWith(reltype, deserialize[T](single))
        }
    }
  }

  /**
   * Get a list of the relationship labels for entities that are
   * dependent on this one, as defined by the @Dependent annotation.
   */
  private def getDependentRelations(cls: EntityClass): List[String] = {
    cls.getMethods.filter { m =>
      m.getAnnotation(classOf[Dependent]) != null
    }.flatMap { m =>
      m.getAnnotation(classOf[Adjacency]) match {
        case null => Nil
        case ann: Adjacency => List(ann.label)
      }
    }.toList
  }

  /**
   * Recusively build a list of all the interfaces a type implements.
   */
  private def getAllInterfaces(cls: Class[_]): Array[Class[_]] = {
    val ifc = cls.getInterfaces()
    ifc ++ ifc.flatMap(ic => ic.getInterfaces())
  }

  /**
   * Load a loopup of entity type name against the equivilent class.
   */
  private def getEntityClasses: Map[String, EntityClass] = {
    // FIXME: This does horrid, fragile stuff to read the model classes
    // from the eu.ehri.projects.models package...
    val classLoader = Thread.currentThread().getContextClassLoader()

    // iterate through all the classes in our models package
    // and filter those that aren't extending VertexFrame
    ClassUtils.getClasses("eu.ehri.project.models").toList.filter { (cls: Class[_]) =>
      // TODO: Make this look at superclasses as well
      getAllInterfaces(cls).toList.contains(classOf[VertexFrame])
    }.flatMap {
      case (entitycls: EntityClass) =>
        entitycls.getAnnotation(classOf[EntityType]) match {
          case ann: EntityType => List[(String, EntityClass)]((ann.value(), entitycls))
          case _ => Nil
        }
    }.toMap
  }
}