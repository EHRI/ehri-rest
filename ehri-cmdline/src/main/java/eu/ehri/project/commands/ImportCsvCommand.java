/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.CsvImportManager;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 *
 * @author linda
 */
public abstract class ImportCsvCommand extends BaseCommand implements Command{
    Class<? extends AbstractImporter<Object>> importer;
    
    public ImportCsvCommand(Class<? extends AbstractImporter<Object>> importer){
        this.importer = importer;
    }
    
    @Override
    protected void setCustomOptions() {
        options.addOption(new Option("scope", true,
                "Identifier of scope to import into, i.e. AuthoritativeSet"));
        options.addOption(new Option("user", true,
                "Identifier of user to import as"));
        options.addOption(new Option("logMessage", false,
                "Log message for import action."));
    }
    
     @Override
    public int execWithOptions(final FramedGraph<Neo4jGraph> graph,
            CommandLine cmdLine) throws Exception {

        GraphManager manager = GraphManagerFactory.getInstance(graph);
        final String logMessage = "Imported from command-line";

        if (cmdLine.getArgList().size() < 1)
            throw new RuntimeException(getHelp());

        List<String> filePaths = new LinkedList<String>();
        for (int i = 0; i < cmdLine.getArgList().size(); i++) {
            filePaths.add((String) cmdLine.getArgList().get(i));
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

            ImportLog log = new CsvImportManager(graph, scope, user, importer).importFiles(filePaths, logMessage);
            
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
}
