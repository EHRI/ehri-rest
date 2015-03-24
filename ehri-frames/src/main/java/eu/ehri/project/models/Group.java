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
package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.javahandler.JavaHandlerContext;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;
import eu.ehri.project.definitions.Entities;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.annotations.Meta;
import eu.ehri.project.models.base.AccessibleEntity;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.IdentifiableEntity;
import eu.ehri.project.models.base.ItemHolder;
import eu.ehri.project.models.base.NamedEntity;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.utils.JavaHandlerUtils;

/**
 * Frame class representing a group of users or other groups
 * that can be assigned permissions.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
@EntityType(EntityClass.GROUP)
public interface Group extends Accessor, AccessibleEntity, IdentifiableEntity,
        PermissionScope, NamedEntity, ItemHolder {
    
    public static final String ADMIN_GROUP_IDENTIFIER = "admin";
    public static final String ANONYMOUS_GROUP_IDENTIFIER = "anonymous";
    public static final String ADMIN_GROUP_NAME = "Administrators";

    /**
     * Fetch the groups to which this group belongs.
     *
     * @return an iterable of group frames
     */
    @Fetch(Ontology.ACCESSOR_BELONGS_TO_GROUP)
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP)
    public Iterable<Group> getGroups();

    /**
     * TODO FIXME use this in case we need AccesibleEnity's instead of Accessors, 
     */
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP, direction = Direction.IN)
    public Iterable<AccessibleEntity> getMembersAsEntities();

    /**
     * Get members of this group.
     *
     * @return an iterable of user or group frames
     */
    @Adjacency(label = Ontology.ACCESSOR_BELONGS_TO_GROUP, direction = Direction.IN)
    public Iterable<Accessor> getMembers();

    /**
     * Get the number of items within this group.
     *
     * @return a count of group members
     */
    @Meta(CHILD_COUNT)
    @JavaHandler
    public long getChildCount();

    /**
     * Adds a Accessor as a member to this Group, so it has the permissions of the Group.
     *
     * @param accessor a user or group frame
     */
    @JavaHandler
    public void addMember(final Accessor accessor);

    /**
     * Removes a member from this group.
     *
     * @param accessor a user or group frame
     */
    @JavaHandler
    public void removeMember(final Accessor accessor);

    /**
     * Get <b>all</b> members of this group, including members of
     * groups within this group.
     *
     * @return an iterable of user or group frames
     */
    @JavaHandler
    public Iterable<AccessibleEntity> getAllUserProfileMembers();

    /**
     * Implementation of complex methods.
     */
    abstract class Impl implements JavaHandlerContext<Vertex>, Group {

        public long getChildCount() {
            return gremlin().inE(Ontology.ACCESSOR_BELONGS_TO_GROUP).count();
        }

        public void addMember(final Accessor accessor) {
            JavaHandlerUtils.addUniqueRelationship(accessor.asVertex(), it(),
                    Ontology.ACCESSOR_BELONGS_TO_GROUP);
        }

        public void removeMember(final Accessor accessor) {
            JavaHandlerUtils.removeAllRelationships(accessor.asVertex(),
                    it(), Ontology.ACCESSOR_BELONGS_TO_GROUP);
        }

        public Iterable<AccessibleEntity> getAllUserProfileMembers() {
            GremlinPipeline<Vertex,Vertex> pipe = gremlin().as("n").in(Ontology.ACCESSOR_BELONGS_TO_GROUP)
                    .loop("n", JavaHandlerUtils.defaultMaxLoops, new PipeFunction<LoopPipe.LoopBundle<Vertex>, Boolean>() {
                        @Override
                        public Boolean compute(LoopPipe.LoopBundle<Vertex> vertexLoopBundle) {
                            return vertexLoopBundle.getObject()
                                    .getProperty(EntityType.TYPE_KEY)
                                    .equals(Entities.USER_PROFILE);
                        }
                    });
            return frameVertices(pipe.dedup());
        }
    }
}
