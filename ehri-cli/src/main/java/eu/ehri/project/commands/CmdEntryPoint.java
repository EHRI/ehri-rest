/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerModule;
import eu.ehri.project.core.Tx;
import eu.ehri.project.core.TxGraph;
import eu.ehri.project.core.impl.TxNeo4jGraph;
import eu.ehri.project.models.utils.CustomAnnotationsModule;
import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Entry point for launching admin commands.
 */
public class CmdEntryPoint extends BaseCommand {

    /**
     * Default return codes for shell tools.
     * <p>
     * 0 means 'OK', 1 'bad arguments', 2 'bad data',
     * 3 'bad permissions'
     */
    public enum RetCode {

        OK(0),
        BAD_ARGS(1),
        BAD_DATA(2),
        BAD_PERMS(3);

        private final int code;

        RetCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * Constructor.
     */
    public CmdEntryPoint() {
        super();
    }

    private static final Map<String, Class<? extends Command>> COMMANDS;

    static {
        Map<String, Class<? extends Command>> mmap = Maps.newHashMap();
        mmap.put(EadImport.NAME, EadImport.class);
        mmap.put(DcImport.NAME, DcImport.class);
        mmap.put(CsvDocDescImport.NAME, CsvDocDescImport.class);
        mmap.put(UshmmEadImport.NAME, UshmmEadImport.class);
        mmap.put(EacImport.NAME, EacImport.class);
        mmap.put(EagImport.NAME, EagImport.class);
        mmap.put(UserListEntities.NAME, UserListEntities.class);
        mmap.put(ListEntities.NAME, ListEntities.class);
        mmap.put(GetEntity.NAME, GetEntity.class);
        mmap.put(LoadFixtures.NAME, LoadFixtures.class);
        mmap.put(Initialize.NAME, Initialize.class);
        mmap.put(GenSchema.NAME, GenSchema.class);
        mmap.put(UserAdd.NAME, UserAdd.class);
        mmap.put(UserMod.NAME, UserMod.class);
        mmap.put(EntityAdd.NAME, EntityAdd.class);
        mmap.put(PersonalitiesImport.NAME, PersonalitiesImport.class);
        mmap.put(DeleteEntities.NAME, DeleteEntities.class);
        mmap.put(RelationAdd.NAME, RelationAdd.class);
        mmap.put(SkosVocabularyImport.NAME, SkosVocabularyImport.class);
        mmap.put(CsvConceptImport.NAME, CsvConceptImport.class);
        // adaptation of UserAdd for adding countries
        mmap.put(CountryAdd.NAME, CountryAdd.class);
        mmap.put(EadAsVirtualCollectionImport.NAME, EadAsVirtualCollectionImport.class);
        mmap.put(GraphSON.NAME, GraphSON.class);
        mmap.put(Check.NAME, Check.class);

        COMMANDS = Collections.unmodifiableMap(mmap);
    }

    @Override
    public String getHelp() {
        try {
            String sep = System.getProperty("line.separator");
            StringBuilder buffer = new StringBuilder(String.format(
                    "Command line interface for the EHRI graph database.%n%n " +
                            "The following commands are available:%n%n"));
            for (String key : Ordering.natural().sortedCopy(COMMANDS.keySet())) {
                Command cmd = COMMANDS.get(key).getConstructor().newInstance();
                buffer.append(String.format("  %-20s %s", key, cmd.getHelp()));
                buffer.append(sep);
            }
            buffer.append(sep);
            buffer.append("Use 'help <command>' for usage details.");
            return buffer.toString();
        } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException
                |InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getUsage() {
        return "Usage: cmd <command> <command-args ... >";
    }

    @Override
    public int execWithOptions(FramedGraph<?> graph, CommandLine cmdLine) throws Exception {
        System.err.println(getHelp());
        return 1;
    }

    public static int run(String[] args) throws Exception {

        if (args.length < 2) {
            return new CmdEntryPoint().exec(null, args);
        } else if (args[1].equals("help")) {
            if (args.length > 2 && COMMANDS.containsKey(args[2])) {
                Command cmd = COMMANDS.get(args[2]).getConstructor().newInstance();
                System.err.println(cmd.getDetailedHelp());
                return RetCode.BAD_ARGS.getCode();
            } else {
                return new CmdEntryPoint().exec(null, args);
            }
        } else {
            if (COMMANDS.containsKey(args[1])) {
                Command cmd = COMMANDS.get(args[1]).getConstructor().newInstance();
                FramedGraph<? extends TxGraph> graph
                        = new FramedGraphFactory(new JavaHandlerModule(), new CustomAnnotationsModule()).create(
                        new TxNeo4jGraph(args[0]));
                try (Tx tx = graph.getBaseGraph().beginTx()) {
                    int res = cmd.exec(graph, Arrays.copyOfRange(args, 2, args.length));
                    if (res == RetCode.OK.getCode()) {
                        tx.success();
                    } else {
                        tx.failure();
                    }
                } catch (MissingArgumentException |MissingOptionException|AlreadySelectedException|
                        UnrecognizedOptionException e) {
                    // options or parameters where not correct, so print the correct usage
                    System.err.println(e.getMessage());
                    System.err.println(cmd.getUsage());
                    return RetCode.BAD_ARGS.getCode();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error: " + e.getMessage());
                    return RetCode.BAD_ARGS.getCode();
                } finally {
                    graph.shutdown();
                }
            } else {
                System.err.println("Unrecognised command: " + args[1]);
                return RetCode.BAD_ARGS.getCode();
            }
        }
        return RetCode.OK.getCode();
    }

    /**
     * Application launcher.
     */
    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }
}
