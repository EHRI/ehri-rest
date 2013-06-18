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

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response toResponse(final BundleError e) {
        Map<String, Object> out = BundleError.getErrorTree(e.getErrors(), e.getRelations());
        try {
            return Response.status(Status.BAD_REQUEST)
                    .entity(mapper.writeValueAsBytes(out)).build();
        } catch (Exception e1) {
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
    }
}
