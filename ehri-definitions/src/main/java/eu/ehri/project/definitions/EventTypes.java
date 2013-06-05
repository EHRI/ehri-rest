package eu.ehri.project.definitions;

/**
 * Types of actions the system supports...
 */
public enum EventTypes {
    creation,
    modification,
    deletion,
    link,
    annotation,
    setGlobalPermissions,
    setItemPermissions,
    setVisibility,
    addGroup,
    removeGroup,
    ingest
}
