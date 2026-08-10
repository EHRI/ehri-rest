/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.acl.wrapper;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.ElementHelper;


import java.util.Set;


public abstract class AclElement implements Element {
    protected Element baseElement;
    protected AclGraph<?> aclGraph;

    protected AclElement(Element baseElement, AclGraph<?> aclGraph) {
        this.baseElement = baseElement;
        this.aclGraph = aclGraph;
    }

    @Override
    public <T> T getProperty(String s) {
        return baseElement.getProperty(s);
    }

    @Override
    public Set<String> getPropertyKeys() {
        return baseElement.getPropertyKeys();
    }

    @Override
    public void setProperty(String s, Object o) {
        baseElement.setProperty(s, o);
    }

    @Override
    public <T> T removeProperty(String s) {
        return baseElement.removeProperty(s);
    }

    @Override
    public void remove() {
        baseElement.remove();
    }

    @Override
    public Object getId() {
        return baseElement.getId();
    }

    @Override
    public String toString() {
        return "[" + getId() + ")]";
    }

    @Override
    public boolean equals(Object object) {
        return ElementHelper.areEqual(this, object);
    }

    @Override
    public int hashCode() {
        // NB: Deliberate decision to ignore
        // accessor when calculating hashCode
        // or equality.
        return baseElement.hashCode();
    }

    public Element getBaseElement() {
        return baseElement;
    }
}
