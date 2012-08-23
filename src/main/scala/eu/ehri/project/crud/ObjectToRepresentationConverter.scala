package eu.ehri.project.crud

import eu.ehri.project.models._

object ObjectToRepresentationConverter {
    def convert(item: Any): String = item match {
      case _: DocumentaryUnit => item.asInstanceOf[DocumentaryUnit].getName
      case _: Agent => item.asInstanceOf[Agent].getName
      case _ => item.toString
    }
}