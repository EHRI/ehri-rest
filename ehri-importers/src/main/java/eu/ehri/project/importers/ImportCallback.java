package eu.ehri.project.importers;

/**
 * Functor class
 * 
 * @author michaelb
 * 
 */
public interface ImportCallback<T> {
    public void itemImported(final T item);
}
