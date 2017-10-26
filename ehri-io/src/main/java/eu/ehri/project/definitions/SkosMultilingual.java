package eu.ehri.project.definitions;

public enum SkosMultilingual implements DefinitionList {

    altLabel(true),
    hiddenLabel(true),
    definition(true),
    note(true),
    scopeNote(true),
    editorialNote(true),
    historyNote(true);

    private final Boolean multiValued;


    SkosMultilingual() {
        this(false);
    }

    SkosMultilingual(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public Boolean isMultiValued() {
        return multiValued;
    }
}
