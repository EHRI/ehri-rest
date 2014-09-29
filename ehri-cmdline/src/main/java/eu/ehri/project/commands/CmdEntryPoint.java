package eu.ehri.project.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.utils.TxCheckedNeo4jGraph;
import org.apache.commons.cli.*;

import java.util.*;

/**
 * Entry point for launching admin commands.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class CmdEntryPoint extends BaseCommand {

    /**
     * Constructor.
     */
    public CmdEntryPoint() {
        super();
    }

    private static final Map<String, Class<? extends Command>> COMMANDS;

    static {
        Map<String, Class<? extends Command>> mmap = new HashMap<String, Class<? extends Command>>();
        mmap.put(EadImport.NAME, EadImport.class);
        mmap.put(DcImport.NAME, DcImport.class);
        mmap.put(IcaAtomEadImport.NAME, IcaAtomEadImport.class);
        mmap.put(UshmmEadImport.NAME, UshmmEadImport.class);
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
        mmap.put(RdfExport.NAME, RdfExport.class);
        mmap.put(RelationAdd.NAME, RelationAdd.class);
        mmap.put(SkosVocabularyImport.NAME, SkosVocabularyImport.class);
        // adaptation of UserAdd for adding countries
        mmap.put(CountryAdd.NAME, CountryAdd.class);
        mmap.put(EadAsVirtualCollectionImport.NAME, EadAsVirtualCollectionImport.class);
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

        help += "\nUse 'help <command>' for usage details.";
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
        } else if (args[1].equals("help")) {
            if (args.length > 2 && CmdEntryPoint.COMMANDS.containsKey(args[2])) {
                Command cmd = CmdEntryPoint.COMMANDS.get(args[2]).getConstructor().newInstance();
                System.err.println(cmd.getHelp());
                return 1;
            } else {
                return new CmdEntryPoint().exec(null, args);
            }
        } else {
            if (CmdEntryPoint.COMMANDS.containsKey(args[1])) {
                Command cmd = CmdEntryPoint.COMMANDS.get(args[1]).getConstructor().newInstance();
                FramedGraph<? extends TransactionalGraph> graph
                        = new FramedGraphFactory(new JavaHandlerModule()).create(
                        new TxCheckedNeo4jGraph(args[0]));
                try {
                    cmd.exec(graph, Arrays.copyOfRange(args, 2, args.length));
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
                } catch (Exception e) {
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
     */
    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }
}
