package eu.ehri.searchindex;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

// from the frames
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.persistance.Bundle;

 
@Path("/indexer")
public class Indexer {

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
	// but can we do XML...?
	// see eu.ehri.project.commands.ListEntities.execWithOptions for inspiration
	//

	/*** A LOT Of TRIAL and TESTING to EXPLORE the technical (im)possibilities ***/
	
	// Test if you can call this RESTfull service
	// 
	// http://localhost:8080/ehri-searchindex/rest/indexer/hello/paul
	@GET
	@Path("/hello/{param}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getMsg(@PathParam("param") String msg) {
 
		String output = "Jersey say : " + msg;
 
		return Response.status(200).entity(output).build();
	}
	
	// Test if you can get 
	// a list of entities of given type; 'userProfile' for instance 
	// in json format from the NEO4J service (must be running!!!)
	//
	// http://localhost:8080/ehri-searchindex/rest/indexer/listjson/userProfile
	@GET
	@Path("/listjson/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getListAsJson(@PathParam("type") String type) {
		String neo4jEhriUrl = "http://localhost:7474/ehri";
		String url = neo4jEhriUrl + "/" + type.trim() + "/paul";//"/list";
		Client client = Client.create();
        WebResource resource = client.resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                //.header("Authorization", "admin")
                .get(ClientResponse.class);

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
        	return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        			.entity("status: " + response.getStatus())
        			.build();
        }
        // Ok, we have json 
        
        String jsonStr = response.getEntity(String.class);

        return Response.status(200).entity(jsonStr).build();        
	}

	// Test if you can get 
	// a list of entities of given type; 'userProfile' for instance 
	// in XML format from the NEO4J service (must be running!!!)
	//
	// http://localhost:8080/ehri-searchindex/rest/indexer/list/userProfile
	@GET
	@Path("/list/{type}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getList(@PathParam("type") String type) {
		String neo4jEhriUrl = "http://localhost:7474/ehri";
		
		// TODO need to fix that conversion to xml does not work with json array yet!
		String url = neo4jEhriUrl + "/" + type.trim() + "/paul";//"/list";
		Client client = Client.create();
        WebResource resource = client.resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
        	return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        			.entity("status: " + response.getStatus())
        			.build();
        }
        // Ok, we have json 
        
        String jsonStr = response.getEntity(String.class);

        //return Response.status(200).entity(jsonStr).build();       

		// Lets convert that json to XML

		try {
			// do the conversion to XML using the ehri-frames lib
			// now we depend on that, possibly nicest if the API could convert for us
			// but how do we enable that without loads of code....
	        Bundle entityBundle = Bundle.fromString(jsonStr);
			return Response.status(200).entity(entityBundle.toXmlString()).build();
		} catch (DeserializationError e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("message: "+ e.getMessage())
					.build();
		}        
	}
	
	// NOTE Only works for a single userProfile!
	@GET
	@Path("/index/{type}")
	@Produces(MediaType.APPLICATION_XML)
	public Response indexType(@PathParam("type") String type) {
		String neo4jEhriUrl = "http://localhost:7474/ehri";
		
		// TODO need to fix that conversion to xml does not work with json array yet
		// until then we need to retrieve a single item instead of a list!
		String url = neo4jEhriUrl + "/" + type.trim() + "/paul";//"/list";
		Client client = Client.create();
        WebResource resource = client.resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
        	return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        			.entity("status: " + response.getStatus())
        			.build();
        }
        // We have json 
        String jsonStr = response.getEntity(String.class);

 		// Lets convert that json to XML
        String xmlStr = "";
		try {
			xmlStr = convertJsonToXml(jsonStr);
		} catch (DeserializationError e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("message: "+ e.getMessage())
					.build();
		}        
		
		// Transform the xml
		try {
			xmlStr = transformXmlForSolr(xmlStr, type);
		} catch (TransformerException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("message: "+ e.getMessage())
					.build();
		}
	
		// TODO update Solr
		// curl $SOLR_URL/update --data-binary @$f -H 'Content-type:application/xml' 
		String solrEhriUrl = "http://localhost:8080/solr-ehri/registry";
		// post to /update
		resource = client.resource(solrEhriUrl + "/update");
        response = resource
                .accept(MediaType.APPLICATION_XML)
                .type(MediaType.APPLICATION_XML)
                .entity(xmlStr)
                .post(ClientResponse.class);

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
        	return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        			.entity("status: " + response.getStatus())
        			.build();
        }
      
        // TODO request for a commit
        // curl $SOLR_URL/update --data-binary '<commit/>' -H 'Content-type:application/xml'

        
		return Response.status(200).entity(response.getEntity(String.class)).build(); 
	}
	
	// Lets convert that json to XML
	// TODO need to fix that conversion to xml does not work with json array yet!
	private String convertJsonToXml(String jsonStr) throws DeserializationError {     
		// do the conversion to XML using the ehri-frames lib
		// now we depend on that, possibly nicest if the API could convert for us
		// but how do we enable that without loads of code....
        Bundle entityBundle = Bundle.fromString(jsonStr);
        String xmlStr = entityBundle.toXmlString();

        // prepend xml instruction
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlStr;
	}
	
	private String transformXmlForSolr(String xmlInStr, String type) throws TransformerException {
		// HARDCODED and hard path for stylesheet location
		String resPath = "/Users/paulboon/Documents/workspace/neo4j-ehri-plugin/ehri-searchindex/src/main/resources";
		// determine stylesheet based on the type
		String xslPath = resPath + "/" + type + "_solr.xsl"; // input xsl	
		
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
		
		return xmlOut.toString();
	}
	
	
	// Test if we can transform XML data to Solr xml with a stylesheet
	// 
	// http://localhost:8080/ehri-searchindex/rest/indexer/trans/paul
	//
	// NOTE transforming is now slightly different in 
	// eu.ehri.project.commands.ListEntities.execWithOptions 
	//
	// NOTE lets try a GET request first, 
	// that transforms and has that as a result (no Solr indexing yet!)
	// 
	// Code below is more like DANS platform is doing it
	//
	@GET
	@Path("/trans/{param}")
	@Produces(MediaType.APPLICATION_XML)
	public Response index(@PathParam("param") String msg) {

		// OPTIONAL 
		// set the TransformFactory to use the Saxon TransformerFactoryImpl method
		//System.setProperty("javax.xml.transform.TransformerFactory",
		//		"net.sf.saxon.TransformerFactoryImpl");

		// HARDCODED and hard paths...
		String resPath = "/Users/paulboon/Documents/workspace/neo4j-ehri-plugin/ehri-searchindex/src/main/resources";
		String sourceID = resPath + "/concepts.xml"; // input xml
		String xslID = resPath + "/cvocConcept_solr.xsl"; // input xsl

		// use a string buffer for the output
		StringWriter buffer = new StringWriter();
		
		try {
			// Create a transform factory instance.
			TransformerFactory tfactory = TransformerFactory.newInstance();

			// Create a transformer for the stylesheet.
			Transformer transformer = tfactory.newTransformer(new StreamSource(
					new File(xslID)));

			// Transform the source XML to System.out.
			transformer.transform(new StreamSource(new File(sourceID)),
					new StreamResult(buffer ));

			return Response.status(200).entity(buffer.toString()).build(); 

		} catch (Exception ex) {
		     System.out.println("EXCEPTION: " + ex);
		     ex.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); 
		}
	}

	
	// TODO Test if we can update Solr with our own xml via its RESTfull API
	// just use some fake xml doc data
	
	
}
