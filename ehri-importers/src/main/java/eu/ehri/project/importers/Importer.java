package eu.ehri.project.importers;

public interface Importer <T> {    
    abstract void importItems() throws Exception;
}
