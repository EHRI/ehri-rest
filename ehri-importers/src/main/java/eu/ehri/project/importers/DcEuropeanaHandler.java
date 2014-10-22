package eu.ehri.project.importers;

import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.properties.XmlImportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * dc files are imported directly beneath the scope provided
 * there is NO structure beneath that
 * @author Linda Reijnhoudt (https://github.com/lindareijnhoudt)
 */
public class DcEuropeanaHandler extends SaxXmlHandler {

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
                    
                    extractIdentifier(currentMap);
                    extractName(currentMap);
                    
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

    private void extractName(Map<String, Object> currentMap) {
        if (!currentMap.containsKey(Ontology.NAME_KEY) && currentMap.containsKey("scopeAndContent")) {
            String name;
            String scope = currentMap.get("scopeAndContent").toString();
            if (scope.length() > 50) {
                if (scope.indexOf(" ", 50) >= 0) {
                    name = scope.substring(0, scope.indexOf(" ", 50));
                } else {
                    name = scope.substring(0, 50);
                }
                name += " ...";
            } else {
                name = scope;
            }
            currentMap.put(Ontology.NAME_KEY, name);
        }
    }

    private void extractIdentifier(Map<String, Object> currentMap) {
        if (currentMap.containsKey("objectIdentifier")){
        logger.debug(currentMap.get("objectIdentifier")+"");
            String id = currentMap.get("objectIdentifier").toString();
            if(id.startsWith("http://www.beeldbankwo2.nl/detail_no.jsp?action=detail&imid=")){
                currentMap.put("objectIdentifier", id.substring(60));
            }
        }else{
            for(String key: currentMap.keySet())
                logger.debug(key);
        }
    }
    
}
