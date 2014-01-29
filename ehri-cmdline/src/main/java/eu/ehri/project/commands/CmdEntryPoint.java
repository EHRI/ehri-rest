package eu.ehri.project.commands;

import com.google.common.collect.Ordering;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.utils.TxCheckedNeo4jGraph;
import org.apache.commons.cli.*;

import java.util.*;

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
        mmap.put(BaEadImport.NAME, BaEadImport.class);
        mmap.put(NiodEadImport.NAME, NiodEadImport.class);
        mmap.put(ItsEadImport.NAME, ItsEadImport.class);
        mmap.put(UshmmEadImport.NAME, UshmmEadImport.class);
        mmap.put(CegesomaEadImport.NAME, CegesomaEadImport.class);
        mmap.put(EacImport.NAME, EacImport.class);
        mmap.put(EagImport.NAME, EagImport.class);
        mmap.put(UserListEntities.NAME, UserListEntities.class);
        mmap.put(ListEntities.NAME, ListEntities.class);
        mmap.put(GetEntity.NAME, GetEntity.class);
        mmap.put(GraphViz.NAME, GraphViz.class);
        mmap.put(LoadFixtures.NAME, LoadFixtures.class);
        mmap.put(Initialize.NAME, Initialize.class);
        mmap.put(Reindex.NAME, Reindex.class);
        mmap.put(UserAdd.NAME, UserAdd.class);
        mmap.put(UserMod.NAME, UserMod.class);
        mmap.put(EntityAdd.NAME, EntityAdd.class);
        mmap.put(PersonalitiesImport.NAME, PersonalitiesImport.class);
        mmap.put(DeleteEntities.NAME, DeleteEntities.class);
        // adaptation of UserAdd for adding countries
        mmap.put(CountryAdd.NAME, CountryAdd.class);

        // new command, could we use reflection code to try find all Command interface implementing classes
        // DISABLED due to brokenness... use GraphSON instead.
        //mmap.put(GraphML.NAME, GraphML.class);
        mmap.put(GraphSON.NAME, GraphSON.class);
        mmap.put(Check.NAME, Check.class);
        
        COMMANDS = Collections.unmodifiableMap(mmap);
    }

    @Override
    public String getHelp() {
        String sep = System.getProperty("line.separator");
        String help = "Command line interface for the EHRI graph database."
                + sep + sep + "The following commands are available:" + sep
                + sep;
        for (String key : Ordering.natural().sortedCopy(CmdEntryPoint.COMMANDS.keySet())) {
            help += "  " + key + sep;
        }
        return help;
    }

    @Override
    public String getUsage() {
    	// "Usage: cmd <graph-db> <command> <command-args ... >": we don't use the <graph-db> option?
        return "Usage: cmd <command> <command-args ... >"; 
    }

    @Override
    public int execWithOptions(FramedGraph<? extends TransactionalGraph> graph, CommandLine cmdLine) throws Exception {
        System.err.println(getHelp());
        return 1;
    }

    public static int run(String[] args) throws Exception {

        if (args.length < 2) {
            return new CmdEntryPoint().exec(null, args);
        } else {
            if (CmdEntryPoint.COMMANDS.containsKey(args[1])) {

                List<String> newArgs = new LinkedList<String>();
                for (int i = 2; i < args.length; i++) {
                    newArgs.add(args[i]);
                }
                Command cmd = CmdEntryPoint.COMMANDS.get(args[1]).getConstructor().newInstance();
                FramedGraph<? extends TransactionalGraph> graph;
                if (cmd.isReadOnly()) {
                    // Get the graph
//                    graph = new FramedGraph<Neo4jGraph>(
//                            new Neo4jGraph(new EmbeddedReadOnlyGraphDatabase(args[0])));
                    /* readonly gives problems on OSX, lets not use it */
                    graph = new FramedGraphFactory(new JavaHandlerModule()).create(
                                new Neo4jGraph(args[0]));
                } else {
                    // Get the graph
                    graph = new FramedGraphFactory(new JavaHandlerModule()).create(
                            new TxCheckedNeo4jGraph(args[0]));
                }

                try {
                    cmd.exec(graph, newArgs.toArray(new String[newArgs.size()]));
                } catch (MissingArgumentException e) {
                	// options or parameters where not correct, so print the correct usage
                    System.err.println(e.getMessage());
                    System.err.println(cmd.getUsage());
                	return 1;
                } catch (MissingOptionException e) {
                    System.err.println(e.getMessage());
                    System.err.println(cmd.getUsage());
                    return 1;
                } catch (AlreadySelectedException e) {
                    System.err.println(e.getMessage());
                    System.err.println(cmd.getUsage());
                    return 1;
                } catch (UnrecognizedOptionException e) {
                    // options or parameters where not correct, so print the correct usage
                    System.err.println(e.getMessage());
                    System.err.println(cmd.getUsage());
                    return 1;
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

    /**
     * Application launcher.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }
}
