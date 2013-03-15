package eu.ehri.project.exceptions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Exception that has a nested structure to represent
 * errors that occur validating or saving a bundle.
 * 
 * @author michaelb
 *
 */
@SuppressWarnings("serial")
public abstract class BundleError extends Exception {
    
    public static final String ERROR_KEY = "errors";
    public static final String REL_KEY = "relationships";
    
    public BundleError(String message) {
        super(message);
    }
    
    public abstract ListMultimap<String, String> getErrors();
    public abstract ListMultimap<String, BundleError> getRelations();

    private static Map<String, List<Object>> getErrorRelations(
            ListMultimap<String, BundleError> rels) {
        Map<String, List<Object>> out = Maps.newHashMap();
        if (rels != null) {
            for (String relation : rels.keys()) {
                LinkedList<Object> errList = Lists.newLinkedList();
                for (BundleError e : rels.get(relation)) {
                    errList.add(e == null ? Maps.newHashMap() : getErrorTree(e.getErrors(), e.getRelations()));
                }
                out.put(relation, errList);
            }
        }
        return out;
    }

    public static Map<String, Object> getErrorTree(ListMultimap<String,String> errors, ListMultimap<String,
            BundleError> relations) {
        return Maps.newHashMap(new ImmutableMap.Builder<String, Object>()
                .put(BundleError.ERROR_KEY, errors.asMap())
                .put(BundleError.REL_KEY, getErrorRelations(relations)).build());
    }
}
