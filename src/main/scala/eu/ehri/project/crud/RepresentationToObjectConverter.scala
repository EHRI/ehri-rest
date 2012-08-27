package eu.ehri.project.crud

import com.tinkerpop.frames.VertexFrame

import scala.collection.JavaConversions._

import eu.ehri.project.persistance._

import eu.ehri.project.core.utils.ClassUtils
import eu.ehri.project.models.annotations.EntityType
import eu.ehri.project.models.EntityTypes

import com.codahale.jerkson.Json

import net.liftweb.json._
import net.liftweb.json.JsonAST.{JValue, JObject}

case class Bundle(val id: Long, val data: JObject, val relationships: Map[String,JValue])

object RepresentationToObjectConverter {
  type EntityClass = Class[_ <: VertexFrame]
  
  lazy val classes: Map[String, EntityClass] = getEntityClasses
  
  implicit def jm2sm(m: java.util.LinkedHashMap[_,_]): Map[_,_] = {
    m.entrySet().toList.map { entry =>
      (entry.getKey(), entry.getValue())    
    }.toMap
  }
  
  def convert[T <: VertexFrame](str: String) = deserialize[T](parse(str))
 
  implicit val formats = Serialization.formats(NoTypeHints)
  
  def deserialize[T <: VertexFrame](data: JValue): UpdateBundle[T] = {
    //val main: Bundle = Bundle(
    //   id = data.getOrElse("id", throw new Exception("No id found in deserialization")).asInstanceOf[java.lang.Integer].toLong,
    //   data = data.getOrElse("data", throw new Exception("No id found in deserialization")).asInstanceOf[Map[String,_]],
    //   relationships = data.getOrElse("relationships", Map[String,Any]()).asInstanceOf[Map[String,Any]]
    //)
    
    val main: Bundle = data.extract[Bundle]
    
    // we must have an isA to tell us the class to instantiate this to.
    val isA: String = main.data.values.getOrElse(EntityTypes.KEY, 
            throw new Exception("Object has no 'isA' field")).asInstanceOf[String]
    val cls = classes.getOrElse(isA, throw new Exception("ISa type '" + isA + "' is not a valid entity type %s".format(classes)))

    // TODO: Deserialize subordinate types. 
    // This may require de-parameterizing the whole lot???
    val bundle = (new BundleFactory[T]).buildBundle(
        main.id,
        main.data.values.asInstanceOf[Map[String,Object]], cls.asInstanceOf[Class[T]])
    main.relationships.foldLeft(bundle) { case (bd, (reltype, data)) =>
      data match {
        case list: List[JValue] => list.foldLeft(bd) { case(bd, (cdata: JValue)) =>
          println("Deserializing type: " + reltype)
          bd.saveWith(reltype, deserialize[T](cdata))
        }
        case single: JValue => bd.saveWith(reltype, deserialize[T](single))
      }
    }
     bundle
  } 
  
  def getAllInterfaces(cls: Class[_]): Array[Class[_]] = { 
    val ifc = cls.getInterfaces()
    ifc ++ ifc.flatMap(ic => ic.getInterfaces())
  }
  
  def getEntityClasses: Map[String,EntityClass] = {
    // FIXME: This does horrid, fragile stuff to read the model classes
    // from the eu.ehri.projects.models package...
    val classLoader = Thread.currentThread().getContextClassLoader()
    
    // iterate through all the classes in our models package
    // and filter those that aren't extending VertexFrame
    ClassUtils.getClasses("eu.ehri.project.models").toList.filter { (cls: Class[_]) =>
      // TODO: Make this look at superclasses as well
      getAllInterfaces(cls).toList.contains(classOf[VertexFrame])
    }.flatMap { case (entitycls: EntityClass) => 
      entitycls.getAnnotation(classOf[EntityType]) match {
        case ann: EntityType => List[(String,EntityClass)]((ann.value(), entitycls))
        case _ => Nil
      }
    }.toMap
  }
}