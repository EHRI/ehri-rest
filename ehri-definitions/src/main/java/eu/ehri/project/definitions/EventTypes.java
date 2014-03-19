package eu.ehri.project.definitions;

/**
 * Types of actions the system supports...
 *
 * Yes, these need renaming!
 *
 */
public enum EventTypes {
    creation,
    createDependent,
    modification,
    modifyDependent,
    deletion,
    deleteDependent,
    link,
    annotation,
    setGlobalPermissions,
    setItemPermissions,
    setVisibility,
    addGroup,
    removeGroup,
    ingest,
    promotion,
    demotion
}
