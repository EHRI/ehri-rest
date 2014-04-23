/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.commands;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.importers.ImportLog;
import eu.ehri.project.importers.SaxImportManager;
import eu.ehri.project.importers.EadIntoVirtualCollectionHandler;
import eu.ehri.project.importers.EadIntoVirtualCollectionImporter;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.VirtualUnit;
import eu.ehri.project.models.base.PermissionScope;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 *
 * this will import an EAD file into a given VirtualUnit.
 * this will NOT import the EAD solely as VirtualUnits, instead DocumentaryUnits will be created for every C-level. 
 * @author linda
 */
public class EadImportIntoVirtualCollection extends EadImport {

    final static String NAME = "ead-import-into-virtualcollection";
    
    public EadImportIntoVirtualCollection() {
        super(EadIntoVirtualCollectionHandler.class, EadIntoVirtualCollectionImporter.class);
    }
    
    @Override
    protected void setCustomOptions() {
        super.setCustomOptions();
        options.addOption(new Option("vc", true,
                "Identifier of virtual collection to import into, i.e. virtual unit"));
    }
    
    @Override
    public String getHelp() {
        return "Usage: " + NAME + " [OPTIONS] -user <user-id> -scope <repository-id> -vc <virtualUnit-id> <ead1.xml> <ead2.xml> ... <eadN.xml>";
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
            
            SaxImportManager importmanager;
            if (cmdLine.hasOption("properties")) {
                XmlImportProperties properties = new XmlImportProperties(cmdLine.getOptionValue("properties"));
                importmanager = new SaxImportManager(graph, scope, user, importer, handler, properties);
            } else {
                importmanager = new SaxImportManager(graph, scope, user, importer, handler);
            }
            /* this is the diff with the ImportCommand */

            // Find the virtual unit this ead should be imported into
            VirtualUnit virtualcollection;
            if (cmdLine.hasOption("virtualcollection")) {
                virtualcollection = manager.getFrame(cmdLine.getOptionValue("virtualcollection"), VirtualUnit.class);
                importmanager.setVirtualCollection(virtualcollection);
            }
            
            importmanager.setTolerant(cmdLine.hasOption("tolerant"));
            ImportLog log = importmanager.importFiles(filePaths, logMessage);
            
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
}
