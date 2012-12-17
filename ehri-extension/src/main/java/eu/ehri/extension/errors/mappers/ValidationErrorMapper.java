package eu.ehri.extension.errors.mappers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.ehri.project.exceptions.BundleError;

/**
 * Serialize a tree of validation errors to JSON. Like bundles,
 * ValidationErrors are a recursive structure with a 'relations'
 * map that contains lists of the errors found in each top-level
 * item's children. The end result should look like:
 * 
 * {
 *   "errors":{},
 *   "relations":{
 *      "describes":[
 *          {}
 *      ],
 *      "hasDate":[
 *          {
 *              "errors":{
 *                  "startDate":["Missing mandatory field"],
 *                  "endDate":["Missing mandatory field"]
 *              },
 *              "relations":{}
 *           }
 *      ]
 *   }
 * }
 * 
 * @author michaelb
 *
 */
@Provider
public class ValidationErrorMapper implements ExceptionMapper<BundleError> {

    private Map<String, List<Object>> getRelations(
            ListMultimap<String, BundleError> rels) {
        Map<String, List<Object>> out = Maps.newHashMap();
        if (rels != null) {
            for (String relation : rels.keys()) {
                LinkedList<Object> errList = Lists.newLinkedList();
                for (BundleError e : rels.get(relation)) {
                    errList.add(e == null ? Maps.newHashMap() : getErrorTree(e));
                }
                out.put(relation, errList);
            }            
        }
        return out;
    }

    private Map<String, Object> getErrorTree(BundleError e) {
        return Maps.newHashMap(new ImmutableMap.Builder<String, Object>()
                .put("errors", e.getErrors().asMap())
                .put("relations", getRelations(e.getRelations())).build());
    }

    @Override
    public Response toResponse(final BundleError e) {
        Map<String, Object> out = getErrorTree(e);
        try {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ObjectMapper().writeValueAsBytes(out)).build();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }
}
