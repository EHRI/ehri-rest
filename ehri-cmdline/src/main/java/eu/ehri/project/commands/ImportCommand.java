/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.SaxImportManager;
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 *
 * @author linda
 */
public abstract class ImportCommand extends BaseCommand implements Command{
    private Class<? extends SaxXmlHandler> handler;
    Class<? extends AbstractImporter> importer;
    
    public ImportCommand(Class<? extends SaxXmlHandler> handler, Class<? extends AbstractImporter> importer){
        this.handler = handler;
        this.importer = importer;
    }
    
    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("scope", true,
                "Identifier of scope to import into, i.e. repository"));
        options.addOption(new Option("F", "files-from", true,
                "Read list of input files from another file (or standard input, if given '-')"));
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
        options.addOption(new Option("tolerant", false,
                "Don't error if a file is not valid."));
        options.addOption(new Option("log", true,
                "Log message for action."));
    }
    
     @Override
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);


        List<String> filePaths = Lists.newArrayList();
        if (cmdLine.hasOption("files-from")) {
            getPathsFromFile(cmdLine.getOptionValue("files-from"), filePaths);
        } else if (cmdLine.getArgList().size() > 0) {
            for (int i = 0; i < cmdLine.getArgList().size(); i++) {
                filePaths.add((String) cmdLine.getArgList().get(i));
            }
        } else {
            throw new RuntimeException(getHelp());
        }

        String logMessage = "Imported from command-line";
        if (cmdLine.hasOption("log")) {
            logMessage = cmdLine.getOptionValue("log");
        }

        try {

            // Find the agent
            PermissionScope scope = SystemScope.getInstance();
            if (cmdLine.hasOption("scope")) {
                scope = manager.getFrame(cmdLine.getOptionValue("scope"), PermissionScope.class);
            }

            // Find the user
            UserProfile user = manager.getFrame(cmdLine.getOptionValue("user"),
                    UserProfile.class);

            ImportLog log = new SaxImportManager(graph, scope, user, importer, handler).setTolerant(cmdLine.hasOption
                    ("tolerant"))
            	.importFiles(filePaths, logMessage);
            //ImportLog log = new SaxImportManager(graph, agent, validUser, EagImporter.class, EagHandler.class).importFile(ios, logMessage);
            
            System.out.println("Import completed. Created: " + log.getCreated()
                    + ", Updated: " + log.getUpdated());
            if (log.getErrored() > 0) {
                System.out.println("Errors:");
                for (Map.Entry<String, String> entry : log.getErrors().entrySet()) {
                    System.out.printf(" - %-20s : %s\n", entry.getKey(),
                            entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        } finally {
            graph.shutdown();
        }
        return 0;
    }

    /**
     * Read a set of file paths from an input, either a file or standard in
     * if given the path '-'.
     * @param listFile
     * @param filePaths
     * @throws Exception
     */
    private void getPathsFromFile(String listFile, List<String> filePaths) throws Exception {
        InputStreamReader reader = listFile.contentEquals("-")
                ? new InputStreamReader(System.in)
                : new FileReader(new File(listFile));
        BufferedReader br = new BufferedReader(reader);
        try {
            String file = null;
            while ((file = br.readLine()) != null) {
                filePaths.add(file);
            }
        } finally {
            br.close();
        }
    }
}
