package eu.ehri.searchindex;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

//from the frames
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistence.Bundle;

// Indexing functionality
//
// On commandline it was:
// 1) get xml from neo4j
// 2) transform with stylesheet
// 3) send to Solr
//
//
// POST /serviceUrl?type=documentaryUnit // update everything with a particular content type
// Nice thing to start trying
// lets see if we can get the documentaryUnit xml data out of a running Neo4j
// - we could use the ehri-extension ?
// The commandline version is: ./scripts/cmd $NEO4J_DBDIR list documentaryUnit --format xml
// but that does not work with a running server...
// we can also get a list of documentaryUnit's with curl:
// curl -v -X GET  -H "Accept: application/json" http://localhost:7474/ehri/documentaryUnit/list
// 
 
@Path("/indexer")
public class Indexer {
    private static Logger logger = LoggerFactory.getLogger(Indexer.class);
	private Configuration config = new Configuration();
/*
	// add a small test for detecting if xml or json was requested
	// @Context UriInfo uriInfo;
	@Context Request request;
	@GET
	@Path("/test/{id}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response test(@PathParam("id") String id) {
		
		return Response.status(200).entity(" id param="+ id + " ").build();
	}
*/
	
	/**
	 * Delete/remove the index for the entity with the given ID
	 * 
	 * @param id
	 * @return
	 */
	@DELETE
	@Path("/index/{id}")
	public Response deleteById(@PathParam("id") String id) {
		// Note: could filter id, to prevent query injection, 
		// but its an internal API so not needed
		return updateSolr("<delete><query>id:" + id + "</query></delete>");		
	}
	
	/**
	 * Index an entity by its ID (same id as from the Graph database)
	 * 
	 * NOTE for the RESTfullness a PUT makes more sense because we change the search index state
	 * 
	 * @param id
	 * @return
	 */
	@GET
	@Path("/index/{id}")
	@Produces(MediaType.APPLICATION_XML)
	public Response indexById(@PathParam("id") String id) {
		
		String url = config.getNeo4jEhriUrl() + "/entities?id=" + id;
		Client client = Client.create();
        WebResource resource = client.resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
        	return getErrorResponse(response);
        }
        String jsonStr = response.getEntity(String.class);
 
        return indexNeo4jJsonResult(jsonStr);
	}
	
	/**
	 * Index all entities that are of the given type
	 * 
	 * @param type
	 * @return
	 */
	@GET
	@Path("/index/type/{type}")
	@Produces(MediaType.APPLICATION_XML)
	public Response indexByType(@PathParam("type") String type) {
		type = type.trim();
		
		// Note about the number of indexed documents. 
		// the number of documents depends on the stylesheet. 
		// For instance when retrieving entities that have descriptions, 
		// the stylesheet will most likely index the descriptions 
		// and that is almost always more that the number of entities. 
		
		// NOTE get all entities and process them, has potential 'out of memory' problems
		// therefore we need to chop it up in chunks
		
		int limit = 32; // some sort of optimal constant
		int offset = 0;
		Response result;
		String jsonStr = "[]";
		do {
			String url = config.getNeo4jEhriUrl() + "/" + type 
					+ "/list?limit=" + limit + "&offset=" + offset;
			Client client = Client.create();
	        WebResource resource = client.resource(url);
	        ClientResponse response = resource
	                .accept(MediaType.APPLICATION_JSON)
	                .get(ClientResponse.class);
	
	        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
	        	return getErrorResponse(response);
	        }
	        jsonStr = response.getEntity(String.class);
	        // Note: if there is nothing we get an empty json list "[]"
	        result = indexNeo4jJsonResult(jsonStr, type, false);
	    	offset += limit; // advance for next chunk	        
		} while (Response.Status.OK.getStatusCode() == result.getStatus() 
				&& !jsonStr.contentEquals("[]")) ;
        
        // commit
        result = commitSolr();
        
        return result;
	}
	
    /*** Helpers ***/
	
	private Response updateSolr(String requestXml) {
		// commit by default
		return updateSolr(requestXml, true); 
	}
	
	private Response updateSolr(String requestXml, boolean commit) {
        // NOTE Solr can be instructed using a GET request with the commit=true
        // But I will stick to the post request for now

		Client client = Client.create();
		// post to /update
		WebResource resource = client.resource(config.getSolrEhriUrl() + "/update");

		ClientResponse response = resource
                .accept(MediaType.APPLICATION_XML)
                .type(MediaType.APPLICATION_XML)
                .entity(requestXml)
                .post(ClientResponse.class);

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
        	return getErrorResponse(response);
        }
        
        if (commit) {
        	return commitSolr();
        } else {
        	// DONT Commit the change
        	// return the Solr response, would like to have ID back
        	return Response.status(200).entity(response.getEntity(String.class)).build(); 	
        }
	}
	
	private Response commitSolr() {
        // NOTE Solr can be instructed using a GET request with the commit=true
        // But I will stick to the post request for now

        // Commit the change
        // curl $SOLR_URL/update --data-binary '<commit/>' -H 'Content-type:application/xml'
		Client client = Client.create();
		// post to /update
		WebResource resource = client.resource(config.getSolrEhriUrl() + "/update");
		ClientResponse response = resource
                .accept(MediaType.APPLICATION_XML)
                .type(MediaType.APPLICATION_XML)
                .entity("<commit/>")
                .post(ClientResponse.class);

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
        	// NOTE what if the change is not commited, there is no rollback?
        	return getErrorResponse(response);
        }
        
        // return the Solr response
		return Response.status(200).entity(response.getEntity(String.class)).build(); 		
	}
	
	private Response indexNeo4jJsonResult (String jsonStr) {
		// commit by default
		return indexNeo4jJsonResult (jsonStr, true);
	}
	
	/**
	 * 
	 * @param jsonStr
	 * @return
	 */
	private Response indexNeo4jJsonResult (String jsonStr, boolean commit) {
		String type = "";
		try {
			type = getTypeFromNeo4jJsonResult(jsonStr);
		} catch (Exception e) {
			return getErrorResponse(e);
		}
		
		return indexNeo4jJsonResult (jsonStr, type, commit);
	}

	private Response indexNeo4jJsonResult (String jsonStr, String type) {
		// commit by default
		return indexNeo4jJsonResult (jsonStr, type, true);
	}
	
	/**
	 * 
	 * @param jsonStr
	 * @param type
	 * @return
	 */
	private Response indexNeo4jJsonResult (String jsonStr, String type, boolean commit) {
 		// Convert that json to XML
        String xmlStr = "";
		try {
			xmlStr = convertJsonListToXml(jsonStr);
		} catch (Exception e) {
			return getErrorResponse(e);
		}        
		
		// Transform the xml
		try {
			xmlStr = transformXmlForSolr(xmlStr, type);
		} catch (TransformerException e) {
			return getErrorResponse(e);
		}

		return updateSolr(xmlStr, commit);
	}
	
	/**
	 * 
	 * @param jsonStr
	 * @return
	 * @throws Exception
	 */
	private String getTypeFromNeo4jJsonResult(String jsonStr) throws Exception {
		// get the type from the JSON and not from the XML after converting
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readValue(jsonStr, JsonNode.class);

		// The node should be a list with at least one item (non-empty) 
		// could check  ID here
		// JsonNode idValue = rootNode.path(0).path(Bundle.ID_KEY); // could check  ID here
		JsonNode typeValue = rootNode.path(0).path(Bundle.TYPE_KEY);
		String type = typeValue.getTextValue();

		logger.debug("Type of entity: " + type);
		return type;
	}
	
	/**
	 * 
	 * @param jsonStr
	 * @return
	 * @throws Exception
	 */
	private String convertJsonListToXml(String jsonStr) throws Exception {
        // get the type from the JSON and not from the XML
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(jsonStr,
			        JsonNode.class);

        String xmlStr = "";
		
        // It's always an list/array iterate?
        // test it jsonNode.isArray() ?
        Iterator<JsonNode> iterator = rootNode.getElements();
        while (iterator.hasNext()) {
			JsonNode jsonNode = iterator.next();
			String itemJsonStr = mapper.writeValueAsString(jsonNode);
			// TODO use stringbuilder

			// convert that json to XML
	        xmlStr += convertJsonToXml(itemJsonStr);
        } 
        
        // prepend xml instruction and wrap in list
        xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<list>" + xmlStr + "</list>";

        return xmlStr;
	}
	
	/**
	 * Convert json to XML
	 * Note that conversion to xml does not work with json array, 
	 * use the convertJsonListToXml instead
	 * 
	 * @param jsonStr
	 * @return
	 * @throws DeserializationError
	 */
	private String convertJsonToXml(String jsonStr) throws DeserializationError {     
		// do the conversion to XML using the ehri-frames lib
		// now we depend on that, possibly nicest if the API could convert for us
		// but how do we enable that without loads of code....
        Bundle entityBundle = Bundle.fromString(jsonStr);
        String xmlStr = entityBundle.toXmlString();

        // DON'T prepend xml instruction
        //return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlStr;
        return xmlStr;
	}
	
	/**
	 * 
	 * @param xmlInStr
	 * @param type
	 * @return
	 * @throws TransformerException
	 */
	private String transformXmlForSolr(String xmlInStr, String type) throws TransformerException {
		logger.debug("\nBefore transform:\n" + xmlInStr);

		// determine stylesheet based on the type
		// try to find the specific stylesheet
		String xslPath = config.getXslLocation() + "/" + type + "_solr.xsl"; // input xsl	
		// test if it exist
		File f = new File(xslPath);
		if(!f.exists()){
			// bail out 
			throw new TransformerException("Stylesheet not found: "+xslPath);
		}

		StringReader xmlIn = new StringReader(xmlInStr);
		StringWriter xmlOut = new StringWriter();		

		// Create a transform factory instance.
		TransformerFactory tfactory = TransformerFactory.newInstance();

		// Create a transformer for the stylesheet.
		Transformer transformer = tfactory.newTransformer(new StreamSource(
				new File(xslPath)));

		// Transform the source XML
		transformer.transform(new StreamSource(xmlIn),
				new StreamResult(xmlOut ));
		
		logger.debug("\nAfter transform:\n" + xmlOut.toString());
		return xmlOut.toString();
	}
	
	/**
	 * 
	 * @param e
	 * @return
	 */
	private Response getErrorResponse(Exception e) {
		// show something anyway
		e.printStackTrace();
		
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity("<message>"+ e.getMessage() + "</message>")
				.build();
	}

	/**
	 * 
	 * @param response
	 * @return
	 */
	private Response getErrorResponse(ClientResponse response) {
    	return Response.status(response.getStatus())
    			.entity(response.getEntity(String.class))
    			.build();
	}
	
	/**
	 * 
	 * @param response
	 * @return
	 */
	private Response getErrorResponse(Response response) {
    	return Response.status(response.getStatus())
    			.entity(response.getEntity())
    			.build();
	}
}
