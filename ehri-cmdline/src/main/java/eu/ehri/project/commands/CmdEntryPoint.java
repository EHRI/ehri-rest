package eu.ehri.project.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

public class CmdEntryPoint extends BaseCommand {

    /**
     * Constructor.
     * 
     */
    public CmdEntryPoint() {
        super();
    }

    private static final Map<String, Class<? extends Command>> COMMANDS;
    static {
        Map<String, Class<? extends Command>> mmap = new HashMap<String, Class<? extends Command>>();
        mmap.put(SkosVocabularyImport.NAME, SkosVocabularyImport.class);
        mmap.put(EadImport.NAME, EadImport.class);
        mmap.put(PersonalitiesImport.NAME, PersonalitiesImport.class);
        mmap.put(EagImport.NAME, EagImport.class);
        mmap.put(UserListEntities.NAME, UserListEntities.class);
        mmap.put(ListEntities.NAME, ListEntities.class);
        mmap.put(GetEntity.NAME, GetEntity.class);
        mmap.put(GraphViz.NAME, GraphViz.class);
        mmap.put(LoadFixtures.NAME, LoadFixtures.class);
        mmap.put(Initialize.NAME, Initialize.class);
        mmap.put(UserAdd.NAME, UserAdd.class);
        mmap.put(UserMod.NAME, UserMod.class);
        mmap.put(EntityAdd.NAME, EntityAdd.class);
        COMMANDS = Collections.unmodifiableMap(mmap);
    }

    @Override
    public String getHelp() {
        String sep = System.getProperty("line.separator");
        String help = "Command line interface for the EHRI graph database."
                + sep + sep + "The following commands are available:" + sep
                + sep;
        for (String key : CmdEntryPoint.COMMANDS.keySet()) {
            help += "  " + key + sep;
        }
        return help;
    }

    @Override
    public String getUsage() {
        return "Usage: cmd <graph-db> <command> <command-args ... >";
    }

    @Override
    public int execWithOptions(FramedGraph<Neo4jGraph> graph, CommandLine cmdLine) throws Exception {
        System.err.println(getHelp());
        return 1;
    }

    public static int main(String[] args) throws Exception {

        if (args.length < 2) {
            return new CmdEntryPoint().exec(null, args);
        } else {
            if (CmdEntryPoint.COMMANDS.containsKey(args[1])) {

                List<String> newArgs = new LinkedList<String>();
                for (int i = 2; i < args.length; i++) {
                    newArgs.add(args[i]);
                }
                Command cmd = CmdEntryPoint.COMMANDS.get(args[1]).getConstructor().newInstance();
                FramedGraph<Neo4jGraph> graph;
                if (cmd.isReadOnly()) {
                    // Get the graph
//                    graph = new FramedGraph<Neo4jGraph>(
//                            new Neo4jGraph(new EmbeddedReadOnlyGraphDatabase(args[0])));
                    /* readonly gives problems on OSX, lets not use it */
                    graph = new FramedGraph<Neo4jGraph>(
                            new Neo4jGraph(args[0]));
                } else {
                    // Get the graph
                    graph = new FramedGraph<Neo4jGraph>(
                            new Neo4jGraph(args[0]));
                }

                try {
                    cmd.exec(graph, newArgs.toArray(new String[newArgs.size()]));
                } catch(Exception e) {
                    e.printStackTrace();
                    System.err.println("Error: " + e.getMessage());
                    return 1;
                } finally {
                    graph.shutdown();
                }
            } else {
                System.err.println("Unrecognised command: " + args[1]);
                return 1;
            }
        }
        return 0;
    }
}
