package eu.ehri.project.persistance

import org.apache.commons.collections.map.MultiValueMap
import java.lang.reflect.Method
import com.tinkerpop.frames._
import eu.ehri.project.models.annotations.EntityType
import eu.ehri.project.exceptions.ValidationError

object Bundle {
  val GET: String = "get"
  val MISSING_PROPERTY: String = "Missing mandatory field"
  val EMPTY_VALUE: String = "No value given for mandatory field"
  val INVALID_ENTITY: String = "No EntityType annotation"
}

trait Bundle {
  def data: Map[String, Any]
}

trait TypedBundle[T] extends Bundle {
  def cls: Class[T]
  def errors: MultiValueMap = new MultiValueMap

  def validate: Unit = {
    checkFields
    if (errors.size() > 0)
      throw new ValidationError(cls, errors)
  }

  def checkFields: Unit = {
    for (method <- cls.getMethods()) {
      for (annotation <- method.getAnnotations()) {
        if (annotation.isInstanceOf[Property] && method.getName.startsWith(Bundle.GET)) {
          checkField(annotation.asInstanceOf[Property].value(), method)
        }
      }
    }
  }

  def checkField(name: String, method: Method): Unit = {
    data.get(name) match {
      case None => errors.put(name, Bundle.MISSING_PROPERTY)
      case null => errors.put(name, Bundle.EMPTY_VALUE)
      case _ =>
    }
    // TODO: Check return type of method matches data...
  }
}
