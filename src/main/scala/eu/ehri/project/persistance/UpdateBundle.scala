package eu.ehri.project.persistance

import com.tinkerpop.frames._

object UpdateBundle {
}

case class UpdateBundle[T](
  val id: Long,
  val data: Map[String, Any],
  val cls: Class[T],
  val relationships: List[Bundle] = Nil) extends TypedBundle[T] {
}