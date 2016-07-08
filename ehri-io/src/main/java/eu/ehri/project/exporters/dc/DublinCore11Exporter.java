package eu.ehri.project.exporters.dc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.api.Api;
import eu.ehri.project.exporters.DocumentWriter;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.utils.LanguageHelpers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DublinCore11Exporter implements DublinCoreExporter {

    private final FramedGraph<?> framedGraph;
    private final Api api;
    private final DocumentBuilder documentBuilder;

    private static final String OAI_NS = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static final String DC_NS = "http://purl.org/dc/elements/1.1/";

    // Mappings of output tags to internal keys.
    private static final Multimap<String, String> propertyMappings = ImmutableMultimap
                .<String, String>builder()
            .putAll("description", Lists.<String>newArrayList("abstract", "scopeAndContent", "biographicalHistory",
                    "history"))
            .putAll("type", Lists.<String>newArrayList("typeOfEntity", "levelOfDescription"))
            .putAll("format", Lists.<String>newArrayList("extentAndMedium"))
            .putAll("language", Lists.<String>newArrayList("languageOfMaterials"))
            .build();

    // A function to transform values with a given tag
    private static final Map<String, Function<Object, String>> valueTransformers = ImmutableMap
                .<String, Function<Object, String>>builder()
            .put("language", s -> LanguageHelpers.codeToName(s.toString()))
            .build();

    public DublinCore11Exporter(final FramedGraph<?> framedGraph, Api api) {
        this.framedGraph = framedGraph;
        this.api = api;
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void export(Described item, OutputStream outputStream, String langCode)
            throws IOException, TransformerException {
        new DocumentWriter(export(item, langCode)).write(outputStream);
    }

    @Override
    public Document export(Described item, String langCode)
            throws IOException {

        // Root
        Document doc = documentBuilder.newDocument();

        Element rootElem = doc.createElementNS(OAI_NS, "oai_dc:dc");
        rootElem.setAttribute("xmlns:oai_dc", OAI_NS);
        rootElem.setAttribute("xmlns:dc", DC_NS);
        rootElem.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        rootElem.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                "xs:schemaLocation", OAI_NS + " http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
        doc.appendChild(rootElem);

        addElement(doc, rootElem, "identifier", item.getIdentifier());

        Optional<Description> descOpt = LanguageHelpers.getBestDescription(
                item, Optional.<Description>absent(), langCode);

        for (Description desc : descOpt.asSet()) {
            addElement(doc, rootElem, "title", desc.getName());

            for (String attr : propertyMappings.keySet()) {
                Collection<String> mappedKeys = propertyMappings.get(attr);
                for (String key : mappedKeys) {
                    Object value = desc.getProperty(key);
                    if (value != null) {
                        if (value instanceof String) {
                            addElement(doc, rootElem, attr, value);
                        } else if (value instanceof List) {
                            for (Object v : (List) value) {
                                addElement(doc, rootElem, attr, v);
                            }
                        }
                        break;
                    }
                }
            }

            for (AccessPoint accessPoint : desc.getAccessPoints()) {
                AccessPointType type = accessPoint.getRelationshipType();
                switch (type) {
                    case creator:
                    case subject:
                        addElement(doc, rootElem, type.name(), accessPoint.getName());
                        break;
                    case person:
                    case corporateBody:
                    case family:
                        addElement(doc, rootElem, "relation", accessPoint.getName());
                        break;
                    case place:
                        addElement(doc, rootElem, "coverage", accessPoint.getName());
                        break;
                    default:
                }
            }
        }

        return doc;
    }

    private void addElement(Document doc, Element parent, String tag, Object value) {
        Element elem = doc.createElementNS(DC_NS, "dc:" + tag);
        String text = valueTransformers.containsKey(tag)
                ? valueTransformers.get(tag).apply(value)
                : value.toString();
        elem.setTextContent(text);
        parent.appendChild(elem);
    }
}
