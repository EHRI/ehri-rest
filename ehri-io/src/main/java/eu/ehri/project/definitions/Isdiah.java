package eu.ehri.project.definitions;

public enum Isdiah implements DefinitionList {

    typeOfEntity,
    otherFormsOfName(true),
    parallelFormsOfName(true),
    history,
    geoculturalContext,
    mandates,
    administrativeStructure,
    records,
    buildings,
    holdings,
    findingAids,
    openingTimes,
    conditions,
    accessibility,
    researchServices,
    reproductionServices,
    publicAreas,
    rulesAndConventions,
    status,
    datesOfDescriptions,
    languages(true),
    scripts(true),
    sources(true),
    maintenanceNotes;

    private final Boolean multiValued;


    Isdiah() {
        this(false);
    }

    Isdiah(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public Boolean isMultiValued() {
        return multiValued;
    }
}
