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

package eu.ehri.project.models.base;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Mandatory;

import java.util.Set;

/**
 * Base interface for all EHRI framed vertex types.
 */
public interface Entity extends VertexFrame {

    /**
     * Cast this frame to another type.
     *
     * @param cls the class of the framed type
     * @param <T> the generic class
     * @return the framed item as the new class
     */
    @JavaHandler
    <T extends Entity> T as(Class<T> cls);

    /**
     * Get the unique item id.
     *
     * @return id
     */
    @Mandatory
    @Property(EntityType.ID_KEY)
    String getId();

    /**
     * Get the type key for this frame.
     *
     * @return type
     */
    @Mandatory
    @Property(EntityType.TYPE_KEY)
    String getType();

    /**
     * Get an arbitrary property from the underlying vertex.
     * @param key the property key
     * @param <T> the property's type
     * @return the property value, or null
     */
    @JavaHandler
    <T> T getProperty(String key);

    @JavaHandler
    <T> T getProperty(Enum<?> key);


    /**
     * Get the property keys from the underlying vertex.
     *
     * @return a set of string keys
     */
    @JavaHandler
    Set<String> getPropertyKeys();

    abstract class Impl implements JavaHandlerContext<Vertex>, Accessible {

        @Override
        public <T extends Entity> T as(Class<T> cls) {
            return frame(it(), cls);
        }

        @Override
        public <T> T getProperty(String key) {
            return it().getProperty(key);
        }

        @Override
        public <T> T getProperty(Enum<?> key) {
            return it().getProperty(key.name());
        }

        @Override
        public Set<String> getPropertyKeys() {
            return it().getPropertyKeys();
        }
    }
}
