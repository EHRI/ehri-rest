package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ValidationError;

public interface Importer<T> {
    abstract void importItems() throws ValidationError;

    public void addCreationCallback(final CreationCallback cb);
}
