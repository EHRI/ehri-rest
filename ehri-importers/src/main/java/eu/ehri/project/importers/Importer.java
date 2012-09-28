package eu.ehri.project.importers;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InvalidInputDataError;

public interface Importer<T> {
    abstract void importItems() throws ValidationError, InvalidInputDataError;

    public void addCreationCallback(final CreationCallback cb);
}
