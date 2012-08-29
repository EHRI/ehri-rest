package eu.ehri.project.views

import com.tinkerpop.frames._
import scala.collection.JavaConversions._
import eu.ehri.project.core.utils.ClassUtils
import eu.ehri.project.models.annotations.{ EntityType, Dependent }

trait DataConverter {
  /**
   * Convenience typedef of a class that is a subtype of VertexFrame
   */
  protected type EntityClass = Class[_ <: VertexFrame]

  /**
   * Lazy-loaded lookup of
   */
  lazy val classes: Map[String, EntityClass] = getEntityClasses

  /**
   * Get a list of the relationship labels for entities that are
   * dependent on this one, as defined by the @Dependent annotation.
   */
  protected def getDependentRelations(cls: EntityClass): List[String] = {
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
  protected def getAllInterfaces(cls: Class[_]): Array[Class[_]] = {
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