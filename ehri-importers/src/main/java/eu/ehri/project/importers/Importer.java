package eu.ehri.project.importers;

public interface Importer <T> {
    
    abstract void importItems(T data) throws Exception;
    abstract void importItem(T data) throws Exception;
}
