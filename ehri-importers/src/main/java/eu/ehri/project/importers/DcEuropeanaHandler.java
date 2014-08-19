/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 *
 * dc files are imported directly beneath the scope provided
 * there is NO structure beneath that
 * @author linda
 */
public class DcEuropeanaHandler extends SaxXmlHandler {


    // Constants for elements we need to watch for.
    private final static String DEFAULT_LANGUAGE = "nld";
    
    protected String eadLanguage = DEFAULT_LANGUAGE;
    private static final Logger logger = LoggerFactory.getLogger(DcEuropeanaHandler.class);

    public DcEuropeanaHandler(AbstractImporter<Map<String, Object>> importer, XmlImportProperties xmlImportProperties) {
        super(importer, xmlImportProperties);
    }


    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //the child closes, add the new DocUnit to the list, establish some relations
        super.endElement(uri, localName, qName);

        if (needToCreateSubNode(qName)) {
             Map<String, Object> currentMap = currentGraphPath.pop();
             if (isUnitDelimiter(qName)) {
                try {
                    //we're back at the top. find the maintenanceevents and add to the topLevel DU
                    currentMap.put("languageCode", "nld");
                    if (currentMap.containsKey("unitDates")) {
                        if (currentMap.get("unitDates") instanceof List) {
                            for (Object d : (List) currentMap.get("unitDates")) {
                                putPropertyInGraph(currentMap, "unitDates", replaceOpname(d.toString()));
                            }
                        } else {
                            putPropertyInGraph(currentMap, "unitDates", replaceOpname(currentMap.get("unitDates").toString()));
                        }

                    }
                    importer.importItem(currentMap, new Stack<String>());
//                importer.importTopLevelExtraNodes(topLevel, current);
                    //importer.importItem(currentGraphPath.pop(), Lists.<String>newArrayList());
                } catch (ValidationError ex) {
                    logger.error(ex.getMessage());
                }
             }else {
                putSubGraphInCurrentGraph(getImportantPath(currentPath), currentMap);
                depth--;
            }
        }

        currentPath.pop();
        
        

    }
    
    @Override
    protected List<String> getSchemas() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected boolean needToCreateSubNode(String qName) {
        boolean need = isUnitDelimiter(qName);
        String path = getImportantPath(currentPath);
        logger.debug(path);
        return need || path.endsWith(EadImporter.ACCESS_POINT);
    }

    private String replaceOpname(String toString) {
         String date =  toString.replace(" (Opname)", "");
                     //dceuropeana
        Pattern dcdate = Pattern.compile("^(\\d{1,2})-(\\d{1,2})-(\\d{4})$");
        Matcher m = dcdate.matcher(date);
        if(m.matches()){
            date = m.group(3)+"-"+m.group(2)+"-"+m.group(1);
        }
        logger.debug(date);
        return date;
    }

    private boolean isUnitDelimiter(String qName) {
        return qName.equals("europeana:record");
    }
}
