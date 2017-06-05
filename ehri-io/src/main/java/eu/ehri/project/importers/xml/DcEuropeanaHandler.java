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

package eu.ehri.project.importers.xml;

import com.google.common.collect.ImmutableMap;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.base.ItemImporter;
import eu.ehri.project.importers.base.SaxXmlHandler;
import eu.ehri.project.importers.ead.EadImporter;
import eu.ehri.project.importers.properties.XmlImportProperties;
import eu.ehri.project.importers.util.ImportHelpers;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.base.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.util.Map;
import java.util.Stack;

/**
 * Dublin Core files are imported directly beneath the scope provided.
 * There is NO structure beneath that.
 */
public class DcEuropeanaHandler extends SaxXmlHandler {

    private static final Logger logger = LoggerFactory.getLogger(DcEuropeanaHandler.class);
private final ImmutableMap<String, Class<? extends Entity>> possibleSubnodes
            = ImmutableMap.<String, Class<? extends Entity>>builder()
                .put("relation", Annotation.class).build();
    public DcEuropeanaHandler(ItemImporter<Map<String, Object>> importer, XmlImportProperties xmlImportProperties) {
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
                    
                    ImportHelpers.putPropertyInGraph(currentMap, "sourceFileId", currentMap.get(ImportHelpers.OBJECT_IDENTIFIER).toString());
                    importer.importItem(currentMap, new Stack<>());
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
    protected boolean needToCreateSubNode(String qName) {
        boolean need = isUnitDelimiter(qName);
        need = need || possibleSubnodes.containsKey(getImportantPath(currentPath));
        String path = getImportantPath(currentPath);
        logger.debug(path);
        return  need || path.endsWith(EadImporter.ACCESS_POINT);
    }

//    private String replaceOpname(String toString) {
//        //done especially for the BBWO2 set
//         String date =  toString.replace(" (Opname)", "").replace(" (Vrijgegeven)", "").replace(" (circa)", "");
//                     //dceuropeana
//        Pattern dcdate = Pattern.compile("^(\\d{1,2})-(\\d{1,2})-(\\d{4})$");
//        Matcher m = dcdate.matcher(date);
//        if(m.matches()){
//            date = m.group(3)+"-"+m.group(2)+"-"+m.group(1);
//        }
//        logger.debug(date);
//        return date;
//    }

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
        if (currentMap.containsKey(ImportHelpers.OBJECT_IDENTIFIER)){
        logger.debug(currentMap.get(ImportHelpers.OBJECT_IDENTIFIER)+"");
            String id = currentMap.get(ImportHelpers.OBJECT_IDENTIFIER).toString();
            if(id.startsWith("http://www.beeldbankwo2.nl/detail_no.jsp?action=detail&imid=")){
                currentMap.put(ImportHelpers.OBJECT_IDENTIFIER, id.substring(60));
            }
        }else{
            for(String key: currentMap.keySet())
                logger.debug(key);
        }
    }
    
}
