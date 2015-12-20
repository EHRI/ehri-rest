// A Cypher script to rename type names for migrating from EHRI DB schema 1 -> 2
// BACKUP THE DATABASE FIRST!
// After this migration has run, do a full internal graph reindex to
// re-sync the global legacy index.

MATCH (n:historicalAgentDescription)
          SET n.__ISA__ = "HistoricalAgentDescription"
          REMOVE n:historicalAgentDescription
          SET n:HistoricalAgentDescription;

MATCH (n:documentaryUnit)
          SET n.__ISA__ = "DocumentaryUnit"
          REMOVE n:documentaryUnit
          SET n:DocumentaryUnit;

MATCH (n:virtualUnit)
          SET n.__ISA__ = "VirtualUnit"
          REMOVE n:virtualUnit
          SET n:VirtualUnit;

MATCH (n:cvocVocabulary)
          SET n.__ISA__ = "CvocVocabulary"
          REMOVE n:cvocVocabulary
          SET n:CvocVocabulary;

MATCH (n:contentType)
          SET n.__ISA__ = "ContentType"
          REMOVE n:contentType
          SET n:ContentType;

MATCH (n:datePeriod)
          SET n.__ISA__ = "DatePeriod"
          REMOVE n:datePeriod
          SET n:DatePeriod;

MATCH (n:repository)
          SET n.__ISA__ = "Repository"
          REMOVE n:repository
          SET n:Repository;

MATCH (n:maintenanceEvent)
          SET n.__ISA__ = "MaintenanceEvent"
          REMOVE n:maintenanceEvent
          SET n:MaintenanceEvent;

MATCH (n:cvocConceptDescription)
          SET n.__ISA__ = "CvocConceptDescription"
          REMOVE n:cvocConceptDescription
          SET n:CvocConceptDescription;

MATCH (n:address)
          SET n.__ISA__ = "Address"
          REMOVE n:address
          SET n:Address;

MATCH (n:documentDescription)
          SET n.__ISA__ = "DocumentaryUnitDescription"
          REMOVE n:documentDescription
          SET n:DocumentaryUnitDescription;

MATCH (n:repositoryDescription)
          SET n.__ISA__ = "RepositoryDescription"
          REMOVE n:repositoryDescription
          SET n:RepositoryDescription;

MATCH (n:systemEvent)
          SET n.__ISA__ = "SystemEvent"
          REMOVE n:systemEvent
          SET n:SystemEvent;

MATCH (n:link)
          SET n.__ISA__ = "Link"
          REMOVE n:link
          SET n:Link;

MATCH (n:authoritativeSet)
          SET n.__ISA__ = "AuthoritativeSet"
          REMOVE n:authoritativeSet
          SET n:AuthoritativeSet;

MATCH (n:annotation)
          SET n.__ISA__ = "Annotation"
          REMOVE n:annotation
          SET n:Annotation;

MATCH (n:historicalAgent)
          SET n.__ISA__ = "HistoricalAgent"
          REMOVE n:historicalAgent
          SET n:HistoricalAgent;

MATCH (n:eventLink)
          SET n.__ISA__ = "EventLink"
          REMOVE n:eventLink
          SET n:EventLink;

MATCH (n:group)
          SET n.__ISA__ = "Group"
          REMOVE n:group
          SET n:Group;

MATCH (n:relationship)
          SET n.__ISA__ = "AccessPoint"
          REMOVE n:relationship
          SET n:AccessPoint;

MATCH (n:country)
          SET n.__ISA__ = "Country"
          REMOVE n:country
          SET n:Country;

MATCH (n:permission)
          SET n.__ISA__ = "Permission"
          REMOVE n:permission
          SET n:Permission;

MATCH (n:system)
          SET n.__ISA__ = "System"
          REMOVE n:system
          SET n:System;

MATCH (n:version)
          SET n.__ISA__ = "Version"
          REMOVE n:version
          SET n:Version;

MATCH (n:cvocConcept)
          SET n.__ISA__ = "CvocConcept"
          REMOVE n:cvocConcept
          SET n:CvocConcept;

MATCH (n:permissionGrant)
          SET n.__ISA__ = "PermissionGrant"
          REMOVE n:permissionGrant
          SET n:PermissionGrant;

MATCH (n:property)
          SET n.__ISA__ = "UnknownProperty"
          REMOVE n:property
          SET n:UnknownProperty;

MATCH (n:userProfile)
          SET n.__ISA__ = "UserProfile"
          REMOVE n:userProfile
          SET n:UserProfile;


// Rename the content types so they match existing types
// This simply entails upper casing the first letter, since
// we're not renaming any content types.
MATCH (ct:ContentType)
    SET ct.__ID__ = upper(substring(ct.__ID__, 0, 1)) + substring(ct.__ID__, 1);