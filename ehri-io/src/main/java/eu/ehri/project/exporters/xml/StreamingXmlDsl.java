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

package eu.ehri.project.exporters.xml;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import eu.ehri.project.importers.util.Helpers;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.List;
import java.util.Map;

/**
 * A helper base class for writing XML as a stream, using
 * lambdas for hierarchical nesting of elements.
 */
public abstract class StreamingXmlDsl {
    protected static Map<String, String> attrs(Object... kv) {
        Preconditions.checkArgument(kv.length % 2 == 0,
                "Attrs must be pairs of key/value");
        Map<String, String> m = Maps.newHashMap();
        for (int i = 0; i < kv.length; i += 2) {
            if (kv[i + 1] != null) {
                m.put(String.valueOf(kv[i]), String.valueOf(kv[i + 1]));
            }
        }
        return m;
    }

    protected static Map<String, String> namespaces(Object... kv) {
        Preconditions.checkArgument(kv.length % 2 == 0,
                "Namespaces must be pairs of key/value");
        return attrs(kv);
    }

    protected void attribute(XMLStreamWriter sw, String ns, String key, String value) {
        try {
            sw.writeAttribute(ns, key, value);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void doc(XMLStreamWriter sw, Runnable runnable) {
        try {
            sw.writeStartDocument();
            sw.writeCharacters("\n");
            runnable.run();
            sw.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void root(XMLStreamWriter sw, String tag, String defaultNamespace,
            Map<String, String> attrs, Map<String, String> namespaces,
            Runnable runnable) {
        try {
            sw.writeStartElement(tag);
            if (defaultNamespace != null) {
                sw.writeDefaultNamespace(defaultNamespace);
            }
            for (Map.Entry<String, String> ns : namespaces.entrySet()) {
                sw.writeNamespace(ns.getKey(), ns.getValue());
            }
            for (Map.Entry<String, String> attr : attrs.entrySet()) {
                sw.writeAttribute(attr.getKey(), attr.getValue());
            }
            runnable.run();
            sw.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void tag(XMLStreamWriter sw, String tag, Runnable runnable) {
        tag(sw, tag, attrs(), runnable);
    }

    protected void tag(XMLStreamWriter sw, String tag, Map<String, String> attrs, Runnable runnable) {
        tag(sw, ImmutableList.of(tag), attrs, runnable);
    }

    protected void tag(XMLStreamWriter sw, List<String> tags, Runnable runnable) {
        tag(sw, tags, attrs(), runnable);
    }

    protected void tag(XMLStreamWriter sw, List<String> tags, Map<String, String> attrs, Runnable runnable) {
        try {
            for (String tag : tags) {
                sw.writeStartElement(tag);
            }
            attributes(sw, attrs);
            runnable.run();
            for (int i = 0; i < tags.size(); i++) {
                sw.writeEndElement();
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void tag(XMLStreamWriter sw, String key, String value) {
        tag(sw, key, value, attrs());
    }

    protected void tag(XMLStreamWriter sw, String key, String value, Map<String, String> attrs) {
        tag(sw, ImmutableList.of(key), value, attrs);
    }

    protected void tag(XMLStreamWriter sw, List<String> keys, String value) {
        tag(sw, keys, value, attrs());
    }

    protected void tag(XMLStreamWriter sw, List<String> keys, String value, Map<String, String> attrs) {
        try {
            for (String key : keys) {
                sw.writeStartElement(key);
            }
            attributes(sw, attrs);
            if (value != null) {
                sw.writeCharacters(value);
            }
            for (int i = 0; i < keys.size(); i++) {
                sw.writeEndElement();
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void comment(XMLStreamWriter sw, String comment) {
        try {
            sw.writeComment(comment);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void characters(XMLStreamWriter sw, String chars) {
        try {
            sw.writeCharacters(chars);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void cData(XMLStreamWriter sw, String chars) {
        try {
            sw.writeCData(Helpers.escapeCData(chars));
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected void attributes(XMLStreamWriter sw, Map<String, String> attrs) {
        try {
            for (Map.Entry<String, String> attr : attrs.entrySet()) {
                if (attr.getValue() != null) {
                    sw.writeAttribute(attr.getKey(), attr.getValue());
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
