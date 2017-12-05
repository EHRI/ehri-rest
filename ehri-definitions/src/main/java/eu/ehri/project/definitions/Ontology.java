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

package eu.ehri.project.definitions;

/**
 * The Ontology contains String constants for properties of graph nodes,
 * some of which will be stored inside nodes as properties and other
 * properties that will be used as labels for links between nodes.
 */
public class Ontology {
    public static final String DOC_HELD_BY_REPOSITORY = "heldBy";
    public static final String REPOSITORY_HAS_COUNTRY = "hasCountry";
    public static final String DESCRIPTION_FOR_ENTITY = "describes";
    public static final String HAS_MAINTENANCE_EVENT = "maintenance"; //links to MaintenanceEvent
    public static final String HAS_ACCESS_POINT = "relatesTo"; //links to UndeterminedRelationship
    public static final String HAS_UNKNOWN_PROPERTY = "hasUnknownProperty";

    // Address
    public static final String ENTITY_HAS_ADDRESS = "hasAddress";

    // Properties
    public static final String CREATION_PROCESS = "creationProcess";
    public static final String LANGUAGE_OF_DESCRIPTION = "languageCode";
    public static final String LANGUAGE = LANGUAGE_OF_DESCRIPTION; //"languageCode";

    public static final String ACCESS_POINT_TYPE = "type";

    // Links
    public static final String LINK_HAS_BODY = "hasLinkBody";
    public static final String LINK_HAS_TARGET = "hasLinkTarget";
    public static final String LINK_HAS_LINKER = "hasLinker";
    public static final String LINK_HAS_TYPE = "type";
    public static final String LINK_HAS_FIELD = "field";
    public static final String LINK_HAS_DESCRIPTION = "description";
    public static final String LINK_HAS_CONTEXT = "inContextOf"; // points to a TL virtual unit

    public static final String HAS_PERMISSION_SCOPE = "hasPermissionScope";
    public static final String IS_ACCESSIBLE_TO = "access";
    public static final String IDENTIFIER_KEY = "identifier";
    public static final String OTHER_IDENTIFIERS = "otherIdentifiers";
    public static final String SOURCEFILE_KEY = "sourceFileId";
    public static final String NAME_KEY = "name";
    public static final String URI_KEY = "uri";
    public static final String PREFLABEL = NAME_KEY; //"prefLabel";

    public static final String DOC_IS_CHILD_OF = "childOf";

    public static final String HISTORICAL_AGENT_CREATED = "created";

    // Permission grants
    public static final String PERMISSION_GRANT_HAS_GRANTEE = "hasGrantee";
    public static final String PERMISSION_GRANT_HAS_SUBJECT = "hasAccessor";
    public static final String PERMISSION_GRANT_HAS_PERMISSION = "hasPermission";
    public static final String PERMISSION_GRANT_HAS_SCOPE = "hasScope";
    public static final String PERMISSION_GRANT_HAS_TARGET = "hasTarget";

    // Annotations
    public static final String ANNOTATES = "annotates";
    public static final String ANNOTATES_PART = "annotatesPart";
    public static final String ANNOTATOR_HAS_ANNOTATION = "hasAnnotation";
    public static final String ANNOTATION_ANNOTATES = "hasAnnotationTarget";
    public static final String ANNOTATION_ANNOTATES_PART = "hasAnnotationTargetPart";
    public static final String ANNOTATION_HAS_SOURCE = "hasAnnotationBody";
    public static final String ANNOTATION_NOTES_BODY = "body";
    public static final String ANNOTATION_FIELD = "field";
    public static final String ANNOTATION_TYPE = "annotationType";

    // Annotation/Link promotion
    public static final String IS_PROMOTABLE = "isPromotable";
    public static final String PROMOTED_BY = "promotedBy";
    public static final String DEMOTED_BY = "demotedBy";

    // Users/groups
    public static final String ACCESSOR_BELONGS_TO_GROUP = "belongsTo";

    // Dates
    public static final String ENTITY_HAS_DATE = "hasDate";
    public static final String DATE_PERIOD_START_DATE = "startDate";
    public static final String DATE_PERIOD_END_DATE = "endDate";
    public static final String DATE_PERIOD_TYPE = "type";
    public static final String DATE_HAS_DESCRIPTION = "description";

    public static final String ITEM_IN_AUTHORITATIVE_SET = "inAuthoritativeSet";

    // Concepts!
    public static final String CONCEPT_HAS_BROADER = "broader";
    public static final String CONCEPT_HAS_NARROWER = "narrower";
    public static final String CONCEPT_HAS_RELATED = "related";

    // System events
    public static final String ENTITY_HAS_EVENT = "hasEvent";
    public static final String ACTION_HAS_EVENT = "actionHasEvent";
    public static final String EVENT_HAS_ACTIONER = "hasActioner";
    public static final String EVENT_HAS_SCOPE = "hasEventScope";
    public static final String EVENT_TIMESTAMP = "timestamp";
    public static final String EVENT_LOG_MESSAGE = "logMessage";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_HAS_FIRST_SUBJECT = "hasFirstSubject";
    public static final String EVENT_PRIOR_VERSION = "priorVersion";
    public static final String ACTIONER_HAS_LIFECYCLE_ACTION = "lifecycleAction";
    public static final String ENTITY_HAS_LIFECYCLE_EVENT = "lifecycleEvent";

    // Versioning
    public static final String ENTITY_HAS_PRIOR_VERSION = "hasPriorVersion";
    public static final String VERSION_HAS_EVENT = "triggeredByEvent";
    public static final String VERSION_ENTITY_CLASS = "entityType";
    public static final String VERSION_ENTITY_ID = "entityId";
    public static final String VERSION_ENTITY_DATA = "entityData";

    // Virtual collections
    public static final String VC_IS_PART_OF = "isPartOf";
    public static final String VC_HAS_AUTHOR = "hasAuthor";
    public static final String VC_DESCRIBED_BY = "isDescribedBy";
    public static final String VC_INCLUDES_UNIT = "includesUnit";
    public static final String VC_IN_COLLECTION = "isInVirtualCollection";

    // User relationships
    public static final String USER_FOLLOWS_USER = "isFollowing";
    public static final String USER_BLOCKS_USER = "isBlocking";
    public static final String USER_WATCHING_ITEM = "isWatching";

    // Maintenance events
    public static final String MAINTENANCE_EVENT_TYPE = "eventType";
    public static final String MAINTENANCE_EVENT_AGENT_TYPE = "agentType";
}
