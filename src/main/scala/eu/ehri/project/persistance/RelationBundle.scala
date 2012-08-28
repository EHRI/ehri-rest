package eu.ehri.project.persistance

case class RelationBundle(
  val data: Map[String, Any] = Map(),
  val label: String,
  val target: Bundle) extends Bundle {
}