package eu.ehri.project.definitions;

public enum Isaar implements DefinitionList {

    lastName,
    firstName,
    source,
    typeOfEntity,
    otherFormsOfName(true),
    parallelFormsOfName(true),
    datesOfExistence,
    biographicalHistory,
    place(true),
    functions(true),
    mandates(true),
    legalStatus,
    structure,
    generalContext,
    scripts(true),
    sources(true),
    occupation;

    private final Boolean multiValued;


    Isaar() {
        this(false);
    }

    Isaar(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public Boolean isMultiValued() {
        return multiValued;
    }
}
