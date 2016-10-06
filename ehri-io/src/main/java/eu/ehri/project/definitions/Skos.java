package eu.ehri.project.definitions;

public enum Skos implements DefinitionList {

    altLabel(true),
    hiddenLabel(true),
    definition(true),
    note(true),
    scopeNote(true),
    editorialNote(true),
    historyNote(true);

    private final Boolean multiValued;


    Skos() {
        this(false);
    }

    Skos(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public Boolean isMultiValued() {
        return multiValued;
    }
}
