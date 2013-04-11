/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;
import eu.ehri.project.importers.exceptions.InvalidXmlDocument;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.persistance.ActionManager;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linda
 */
public class CsvImportManager extends XmlImportManager{
    private static final Logger logger = LoggerFactory.getLogger(CsvImportManager.class);
    
    private AbstractImporter<Map<String, Object>> importer;
    private  Class<? extends AbstractImporter> importerClass;


    public CsvImportManager(FramedGraph<Neo4jGraph> framedGraph,
            final PermissionScope permissionScope, final Actioner actioner, Class<? extends AbstractImporter> importerClass) {
        super(framedGraph, permissionScope, actioner);
        this.importerClass = importerClass;
    }

    /**
     * Import XML from the given InputStream, as part of the given action.
     *
     * @param ios
     * @param eventContext
     * @param log
     *
     * @throws IOException
     * @throws ValidationError
     * @throws InputParseError
     * @throws InvalidInputFormatError
     * @throws InvalidXmlDocument
     */
    @Override
    protected void importFile(InputStream ios, final ActionManager.EventContext eventContext,
            final ImportLog log) throws IOException, ValidationError,
            InputParseError, InvalidXmlDocument, InvalidInputFormatError {

        try {
            importer = importerClass.getConstructor(FramedGraph.class, PermissionScope.class,
                    ImportLog.class).newInstance(framedGraph, permissionScope, log);
            logger.info("importer of class " + importer.getClass());
            importer.addCreationCallback(new ImportCallback() {
                @Override
                public void itemImported(AccessibleEntity item) {
                    logger.info("ImportCallback: itemImported creation " + item.getId());
                    eventContext.addSubjects(item);
                    log.addCreated();
                }
            });
            importer.addUpdateCallback(new ImportCallback() {
                @Override
                public void itemImported(AccessibleEntity item) {
                    logger.info("ImportCallback: itemImported updated");
                    eventContext.addSubjects(item);
                    log.addUpdated();
                }
            });
            //TODO: read the actual contents of the file, 
            Scanner s = new Scanner(ios, "UTF-8").useDelimiter("\\n");
            String[] headers=null;
            if(s.hasNext()){
                headers = s.next().split(";");
                for(int i = 0 ; i < headers.length; i++){
                    headers[i] = headers[i].replaceAll("\\s", "");
                }
            }
            if(headers == null){
                throw new InvalidInputFormatError("no content found");
            }
//            importer.checkProperties(headers);
            //per record, call importer.importItem(Map<String, Object> itemData 
            while(s.hasNext()){
                importer.importItem(createItem(s.next(), headers));
            }
        } catch (InstantiationException ex) {
            logger.error("InstantiationException: "+ex.getMessage());
        } catch (IllegalAccessException ex) {
            logger.error("IllegalAccess: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("IllegalArgumentException: "+ex.getMessage());
        } catch (InvocationTargetException ex) {
            logger.error("InvocationTargetException: "+ ex.getMessage());
        } catch (NoSuchMethodException ex) {
            logger.error("NoSuchMethodException: "+ ex.getMessage());
        } catch (SecurityException ex) {
            logger.error("SecurityException: "+ ex.getMessage());
        }

    }

    private Map<String, Object> createItem(String input, String[] headers) throws InvalidInputFormatError {
        //we cannot just split on ';', because it could be part of the value, 
        //do a quick scan if we can, otherwise, the hard way
        String[] values = input.split(";");
        if(values.length !=  headers.length){
            //search for "text; text"
            String[] realvalues = new String[headers.length];
            int j=0;
            for(int i = 0; i<values.length; i++){
                if(values[i].startsWith("\"")){
                    realvalues[j]=values[i];
                    //find the next "
                    for(int k = i ; k < values.length; k++){
                        realvalues[j] += ";"+values[k];
                        if(values[k].endsWith("\"")){
                            i=k;
                            break;
                        }
                    }
                }else{
                    realvalues[j]=values[i];
                }
                j++;
            }
            values=realvalues;
        }
        
        assert(values.length == headers.length);
        if(values.length != headers.length)
            throw new InvalidInputFormatError("number of headers unequal to number of values in " + values.toString());
        Map<String, Object> map = new HashMap<String, Object>();
        //just put the headers and the values in the map
        //the mapping to the properties will be done by the Importer itself
        for(int h = 0; h < headers.length; h++){
            map.put(headers[h], values[h]);
        }
        return map;
        
    }

}
