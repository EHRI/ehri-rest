// A Cypher script to rename type names for migrating from EHRI DB schema 1 -> 2
// BACKUP THE DATABASE FIRST!
// After this migration has run, do a full internal graph reindex to
// re-sync the global legacy index.

MATCH (n:historicalAgentDescription)
          SET n.__type = "HistoricalAgentDescription"
          REMOVE n:historicalAgentDescription
          SET n:HistoricalAgentDescription;

MATCH (n:documentaryUnit)
          SET n.__type = "DocumentaryUnit"
          REMOVE n:documentaryUnit
          SET n:DocumentaryUnit;

MATCH (n:virtualUnit)
          SET n.__type = "VirtualUnit"
          REMOVE n:virtualUnit
          SET n:VirtualUnit;

MATCH (n:cvocVocabulary)
          SET n.__type = "CvocVocabulary"
          REMOVE n:cvocVocabulary
          SET n:CvocVocabulary;

MATCH (n:contentType)
          SET n.__type = "ContentType"
          REMOVE n:contentType
          SET n:ContentType;

MATCH (n:datePeriod)
          SET n.__type = "DatePeriod"
          REMOVE n:datePeriod
          SET n:DatePeriod;

MATCH (n:repository)
          SET n.__type = "Repository"
          REMOVE n:repository
          SET n:Repository;

MATCH (n:maintenanceEvent)
          SET n.__type = "MaintenanceEvent"
          REMOVE n:maintenanceEvent
          SET n:MaintenanceEvent;

MATCH (n:cvocConceptDescription)
          SET n.__type = "CvocConceptDescription"
          REMOVE n:cvocConceptDescription
          SET n:CvocConceptDescription;

MATCH (n:address)
          SET n.__type = "Address"
          REMOVE n:address
          SET n:Address;

MATCH (n:documentDescription)
          SET n.__type = "DocumentaryUnitDescription"
          REMOVE n:documentDescription
          SET n:DocumentaryUnitDescription;

MATCH (n:repositoryDescription)
          SET n.__type = "RepositoryDescription"
          REMOVE n:repositoryDescription
          SET n:RepositoryDescription;

MATCH (n:systemEvent)
          SET n.__type = "SystemEvent"
          REMOVE n:systemEvent
          SET n:SystemEvent;

MATCH (n:link)
          SET n.__type = "Link"
          REMOVE n:link
          SET n:Link;

MATCH (n:authoritativeSet)
          SET n.__type = "AuthoritativeSet"
          REMOVE n:authoritativeSet
          SET n:AuthoritativeSet;

MATCH (n:annotation)
          SET n.__type = "Annotation"
          REMOVE n:annotation
          SET n:Annotation;

MATCH (n:historicalAgent)
          SET n.__type = "HistoricalAgent"
          REMOVE n:historicalAgent
          SET n:HistoricalAgent;

MATCH (n:eventLink)
          SET n.__type = "EventLink"
          REMOVE n:eventLink
          SET n:EventLink;

MATCH (n:group)
          SET n.__type = "Group"
          REMOVE n:group
          SET n:Group;

MATCH (n:relationship)
          SET n.__type = "AccessPoint"
          REMOVE n:relationship
          SET n:AccessPoint;

MATCH (n:country)
          SET n.__type = "Country"
          REMOVE n:country
          SET n:Country;

MATCH (n:permission)
          SET n.__type = "Permission"
          REMOVE n:permission
          SET n:Permission;

MATCH (n:system)
          SET n.__type = "System"
          REMOVE n:system
          SET n:System;

MATCH (n:version)
          SET n.__type = "Version"
          REMOVE n:version
          SET n:Version;

MATCH (n:cvocConcept)
          SET n.__type = "CvocConcept"
          REMOVE n:cvocConcept
          SET n:CvocConcept;

MATCH (n:permissionGrant)
          SET n.__type = "PermissionGrant"
          REMOVE n:permissionGrant
          SET n:PermissionGrant;

MATCH (n:property)
          SET n.__type = "UnknownProperty"
          REMOVE n:property
          SET n:UnknownProperty;

MATCH (n:userProfile)
          SET n.__type = "UserProfile"
          REMOVE n:userProfile
          SET n:UserProfile;


// Rename the content types so they match existing types
// This simply entails upper casing the first letter, since
// we're not renaming any content types.
MATCH (ct:ContentType)
    SET ct.__id = upper(substring(ct.__id, 0, 1)) + substring(ct.__id, 1);