/*
 * Copyright 2022 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
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

package eu.ehri.project.importers.properties;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class NodeProperties {

    public final static String NODE = "node";
    public final static String PROPERTY = "property";
    public final static String MULTIVALUED = "multivalued?";
    public final static String REQUIRED = "required?";
    public final static String HANDLERNAME = "handlerTempName";
    private final List<String> titles;
    public static final String SEP = ",";
    private final Map<String, List<PropertiesRow>> p;
    private final Set<String> allKnownNodes;

    public NodeProperties() {
        titles = Lists.newArrayList();
        p = Maps.newHashMap();
        allKnownNodes = Sets.newHashSet();
    }

    public void setTitles(String titles) {
        this.titles.addAll(Arrays.asList(titles.split(SEP)));
        assert (titles.contains(NODE));
        assert (titles.contains(PROPERTY));
        assert (titles.contains(MULTIVALUED));
        assert (titles.contains(REQUIRED));
        assert (titles.contains(HANDLERNAME));
    }

    private PropertiesRow createPropertiesCheck(String row) {
        int i = 0;
        PropertiesRow c = new PropertiesRow();
        for (String r : row.split(SEP)) {
            c.add(titles.get(i), r);
            i++;
        }
        return c;
    }

    public void addRow(String row) {
        String nodetype = row.split(SEP)[0];
        allKnownNodes.add(nodetype);
        if (!p.containsKey(nodetype)) {
            p.put(nodetype, Lists.<PropertiesRow>newArrayList());
        }
        p.get(nodetype).add(createPropertiesCheck(row));
    }

    private PropertiesRow getProperty(String nodetype, String property) {
        if (!p.containsKey(nodetype)) {
            return null;
        }
        List<PropertiesRow> l = p.get(nodetype);
        for (PropertiesRow pr : l) {
            if (property.equals(pr.get(PROPERTY))) {
                return pr;
            }
        }
        return null;
    }

    public boolean hasProperty(String nodetype, String property) {
        PropertiesRow pr = getProperty(nodetype, property);
        return pr != null;
    }

    protected String getHandlerName(String nodetype, String property) {
        return getProperty(nodetype, property).get(HANDLERNAME);
    }

    public boolean isMultivaluedProperty(String nodetype, String property) {
        return isBooleanFieldTrue(nodetype, property, MULTIVALUED);
    }

    protected boolean isRequiredProperty(String nodetype, String property) {
        return isBooleanFieldTrue(nodetype, property, REQUIRED);
    }

    private boolean isBooleanFieldTrue(String nodetype, String property, String booleanField) {
        PropertiesRow pr = getProperty(nodetype, property);
        if (pr == null) {
            return false;
        }
        return "1".equals(pr.get(booleanField));
    }

    protected Set<String> getRequiredProperties(String nodetype) {
        List<PropertiesRow> parentrows = getReferencedProperties(nodetype);
        if (parentrows == null || p.get(nodetype) == null)
            return null;
        parentrows.addAll(p.get(nodetype));

        Set<String> required = Sets.newHashSet();
        for (PropertiesRow r : parentrows) {
            if ("1".equals(r.get(REQUIRED))) {

                if (r.get(HANDLERNAME) == null) {
                    required.add(r.get(PROPERTY));
                } else {
                    required.add(r.get(HANDLERNAME));
                }
            }
        }
        return required;
    }

    /**
     * @param nodetype the type of node
     * @return returns a list of properties that were referenced from this node-type, like
     * address or description
     */
    private List<PropertiesRow> getReferencedProperties(String nodetype) {
        List<PropertiesRow> referencedrows = Lists.newArrayList();
        for (PropertiesRow prop : p.get(nodetype)) {
            if (allKnownNodes.contains(prop.get(PROPERTY))) {
                for (PropertiesRow pr : p.get(prop.get(PROPERTY))) {
                    referencedrows.add(pr.clone().add(PROPERTY, prop.get(PROPERTY) + "/" + pr.get(PROPERTY)));
                }
                referencedrows.addAll(p.get(prop.get(PROPERTY)));
            }
        }
        if (nodetype.endsWith("Description")) {
            referencedrows.addAll(p.get("Unit"));
            referencedrows.addAll(p.get("Description"));
        }
        return referencedrows;
    }

    protected Set<String> getHandlerProperties(String node) {
        List<PropertiesRow> parentrows = getReferencedProperties(node);
        if (parentrows == null || p.get(node) == null)
            return null;
        parentrows.addAll(p.get(node));

        Set<String> required = Sets.newHashSet();
        for (PropertiesRow r : parentrows) {
            if (r.get(HANDLERNAME) == null) {
                required.add(r.get(PROPERTY));
            } else {
                required.add(r.get(HANDLERNAME));
            }
        }
        return required;
    }
}
