package eu.ehri.project.persistance;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.Action;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;

/**
 * Class for dealing with actions.
 * 
 * @author michaelb
 *
 */
public class ActionManager {
    
    protected FramedGraph<Neo4jGraph> graph;

    /**
     * Constructor.
     * 
     * @param graph
     */
    public ActionManager(FramedGraph<Neo4jGraph> graph) {
        this.graph = graph;
    }

    /**
     * Create an action node describing something that user U
     * has done.
     * 
     * @param user
     * @param logMessage
     * @return
     */
    public Action createAction(Actioner user, String logMessage) {
        Map<String,Object> actionData = new HashMap<String, Object>();
        actionData.put("timestamp", getTimestamp());
        actionData.put("logMessage", logMessage);
        
        BundleDAO<Action> persister = new BundleDAO<Action>(graph);
        Action action;
        try {
            action = persister.insert(new BundleFactory<Action>().buildBundle(actionData, Action.class));
        } catch (ValidationError e) {            
            e.printStackTrace();
            throw new RuntimeException("Unexpected error creating action", e);
        }
        action.setActioner(user);
        return action;        
    }

    /**
     * Create an action node that describes what user U has done with
     * subject S via logMessage log.
     * 
     * @param subject
     * @param user
     * @param logMessage
     * @return
     */
    public Action createAction(AccessibleEntity subject, Actioner user, String logMessage) {
        Action action = createAction(user, logMessage);
        action.setSubject(subject);
        return action;        
    }
    
    // Helpers.
    
    private String getTimestamp() {
        DateTime dt = DateTime.now();
        return ISODateTimeFormat.dateTime().print(dt);
    }
}
