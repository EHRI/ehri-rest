package eu.ehri.project.definitions;


import java.util.Optional;

public enum IsadG implements DefinitionList {

    archivistNote("3.7.1"),
    archivalHistory("3.2.3"),
    acquisition("3.2.4"),
    appraisal("3.3.2"),
    accruals("3.3.3"),
    biographicalHistory("3.2.2"),
    conditionsOfAccess("3.4.1"),
    conditionsOfReproduction("3.4.2"),
    //dates,
    datesOfDescriptions("3.7.3"),
    extentAndMedium("3.1.5"),
    findingAids("3.4.5", true),
    languageOfMaterial("3.4.3", true),
    levelOfDescription("3.1.4"),
    locationOfOriginals("3.5.1", true),
    locationOfCopies("3.5.2", true),
    relatedUnitsOfDescription("3.5.3"),
    parallelFormsOfName("3.1.2", true),
    physicalCharacteristics("3.4.4"),
    physicalLocation("3.4.4", true),
    publicationNote("3.5.4"),
    notes("3.6.1", true),
    ref("3.7.1"),
    rulesAndConventions("3.7.2"),
    scopeAndContent("3.3.1"),
    scriptOfMaterial("3.4.3", true),
    separatedUnitsOfDescription("3.5.3"),
    sources("3.7.1", true),
    systemOfArrangement("3.3.4"),
    unitDates("3.1.3", true);

    private final Boolean multiValued;
    private final String analogueEncoding;


    IsadG() {
        this(null, false);
    }

    IsadG(Boolean multiValued) {
        this(null, multiValued);
    }

    IsadG(String analogueEncoding) {
        this(analogueEncoding, false);
    }

    IsadG(String analogueEncoding, Boolean multiValued) {
        this.multiValued = multiValued;
        this.analogueEncoding = analogueEncoding;
    }

    public Boolean isMultiValued() {
        return multiValued;
    }

    public Optional<String> getAnalogueEncoding() {
        return Optional.ofNullable(analogueEncoding);
    }
}
