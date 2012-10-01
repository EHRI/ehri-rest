package eu.ehri.project.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;

public class CmdEntryPoint extends BaseCommand {

    /**
     * Constructor.
     * 
     * @param args
     * @throws ParseException
     */
    public CmdEntryPoint() {
        super();
    }

    private static final Map<String, Class<? extends Command>> COMMANDS;
    static {
        Map<String, Class<? extends Command>> mmap = new HashMap<String, Class<? extends Command>>();
        mmap.put(EadImport.NAME, EadImport.class);
        mmap.put(ListEntities.NAME, ListEntities.class);
        mmap.put(LoadFixtures.NAME, LoadFixtures.class);
        COMMANDS = Collections.unmodifiableMap(mmap);
    };

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
        return "Usage: ehri <graph-db> <command> <command-args ... >";
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

                // Get the graph
                FramedGraph<Neo4jGraph> graph = new FramedGraph<Neo4jGraph>(
                        new Neo4jGraph((String) args[0]));

                List<String> newArgs = new LinkedList<String>();
                for (int i = 2; i < args.length; i++) {
                    newArgs.add(args[i]);
                }
                Command cmd = CmdEntryPoint.COMMANDS.get(args[1]).getConstructor().newInstance();
                
                try {                    
                    cmd.exec(graph, newArgs.toArray(new String[newArgs.size()]));
                } catch(Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    return 1;
                } finally {
                    graph.shutdown();
                }
            }
        }
        return 0;
    }
}
