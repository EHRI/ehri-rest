package eu.ehri.extension.errors.mappers;

import com.google.common.base.Charsets;
import eu.ehri.project.exceptions.ValidationError;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


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
 * @author Mike Bryant (http://github.com/mikesname)
 *
 */
@Provider
public class ValidationErrorMapper implements ExceptionMapper<ValidationError> {
    @Override
    public Response toResponse(final ValidationError e) {
        try {
            return Response.status(Status.BAD_REQUEST)
                    .entity(e.getErrorSet().toJson()
                            .getBytes(Charsets.UTF_8)).build();
        } catch (Exception e1) {
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
    }
}
