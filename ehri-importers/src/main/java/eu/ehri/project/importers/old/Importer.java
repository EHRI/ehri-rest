package eu.ehri.project.importers.old;

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.ImportCallback;
import eu.ehri.project.importers.exceptions.InvalidInputFormatError;

public interface Importer<T> {
    abstract void importItems() throws ValidationError, InvalidInputFormatError;

    public void addCreationCallback(final ImportCallback cb);
}
