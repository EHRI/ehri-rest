package eu.ehri.project.commands;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.AbstractImporter;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.SaxImportManager;
import eu.ehri.project.importers.SaxXmlHandler;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.PermissionScope;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public abstract class ImportCommand extends BaseCommand implements Command{
    protected Class<? extends SaxXmlHandler> handler;
    protected Class<? extends AbstractImporter> importer;
    
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
        options.addOption(new Option("properties", true,
                "Provide another property file (default depends on HandlerClass)"));
    }
    
     @Override
    public int execWithOptions(final FramedGraph<? extends TransactionalGraph> graph,
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
            
            ImportLog log;

            if (cmdLine.hasOption("properties")) {
                XmlImportProperties properties = new XmlImportProperties(cmdLine.getOptionValue("properties"));
                logMessage += "Using properties file : " + cmdLine.getOptionValue("properties");
                log = new SaxImportManager(graph, scope, user, importer, handler, properties).setTolerant(cmdLine.hasOption("tolerant")).importFiles(filePaths, logMessage);
            } else {
                log = new SaxImportManager(graph, scope, user, importer, handler).setTolerant(cmdLine.hasOption("tolerant")).importFiles(filePaths, logMessage);
            }
            
            System.out.println("Import completed. Created: " + log.getCreated()
                    + ", Updated: " + log.getUpdated() + ", Unchanged: " + log.getUnchanged());
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
        }
        return 0;
    }

    /**
     * Read a set of file paths from an input, either a file or standard in
     * if given the path '-'.
     * @param listFile A path to a local file
     * @param filePaths An output parameter for file paths contained in
     *                  the given file.
     * @throws Exception
     */
    protected void getPathsFromFile(String listFile, List<String> filePaths) throws Exception {
        InputStreamReader reader = listFile.contentEquals("-")
                ? new InputStreamReader(System.in)
                : new FileReader(new File(listFile));
        BufferedReader br = new BufferedReader(reader);
        try {
            String file;
            while ((file = br.readLine()) != null) {
                filePaths.add(file);
            }
        } finally {
            br.close();
        }
    }
}
