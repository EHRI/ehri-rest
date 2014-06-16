
package eu.ehri.project.importers.cvoc;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.cvoc.Vocabulary;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SkosImporterFactory {
    public static SkosImporter newSkosImporter(FramedGraph<? extends TransactionalGraph> graph,
            Actioner actioner, Vocabulary vocabulary) {
        //String property = System.getProperty("eu.ehri.project.importers.SkosImporter");
        // TODO: Load dynamically via system prop
        return new JenaSkosImporter(graph, actioner, vocabulary);
    }

}
