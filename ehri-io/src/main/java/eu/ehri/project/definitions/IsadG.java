package eu.ehri.project.definitions;

public enum IsadG implements DefinitionList {

    archivistNote,
    archivalHistory,
    acquisition,
    appraisal,
    accruals,
    biographicalHistory,
    conditionsOfAccess,
    conditionsOfReproduction,
    //dates,
    datesOfDescriptions,
    extentAndMedium,
    findingAids(true),
    languageOfMaterial(true),
    levelOfDescription,
    locationOfOriginals(true),
    locationOfCopies(true),
    relatedUnitsOfDescription,
    physicalCharacteristics,
    physicalLocation(true),
    publicationNote,
    notes(true),
    rulesAndConventions,
    scopeAndContent,
    scriptOfMaterial(true),
    separatedUnitsOfDescription,
    sources(true),
    systemOfArrangement,
    unitDates(true);

    private final Boolean multiValued;


    IsadG() {
        this(false);
    }

    IsadG(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public Boolean isMultiValued() {
        return multiValued;
    }
}
