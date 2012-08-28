package eu.ehri.project.persistance

case class InsertBundle[T](
  val data: Map[String,Any],
  val cls: Class[T],
  val relationships: List[RelationBundle] = Nil
) extends TypedBundle[T] {

}