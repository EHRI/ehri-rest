package eu.ehri.project.definitions;

public enum ContactInfo implements DefinitionList {

    addressName,
    contactPerson,
    street,
    municipality,
    firstdem,
    countryCode,
    postalCode,
    email(true),
    telephone(true),
    fax(true),
    webpage(true);

    private final Boolean multiValued;

    ContactInfo() {
        this(false);
    }

    ContactInfo(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public Boolean isMultiValued() {
        return multiValued;
    }
}
