package eu.ehri.project.importers;

import java.util.List;

import eu.ehri.project.persistance.EntityBundle;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.*;

public interface Importer <T> {
    
    public void importDocumentaryUnit(T data) throws Exception;
    abstract void importDetails(T data) throws Exception;
    abstract EntityBundle<DocumentaryUnit> extractDocumentaryUnit(T data) throws Exception;
    abstract List<EntityBundle<DocumentDescription>> extractDocumentDescriptions(T data) throws Exception;
    abstract List<EntityBundle<DocumentaryUnit>> extractParent(T data) throws Exception;
    abstract List<EntityBundle<Authority>> extractAuthorities(T data) throws Exception;
    abstract List<EntityBundle<DatePeriod>> extractDates(T data) throws Exception;
}
