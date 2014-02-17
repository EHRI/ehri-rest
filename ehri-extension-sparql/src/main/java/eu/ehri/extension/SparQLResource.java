package eu.ehri.extension;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.blueprints.oupls.sail.pg.PropertyGraphSail;
import info.aduna.iteration.CloseableIteration;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailTupleQuery;
import org.openrdf.sail.Sail;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * Created by michaelb on 10/06/13.
 * <p/>
 * Resource for executing SparQL queries on the graph
 * database.
 * <p/>
 * * Insecure and SHOULD NOT be a public endpoint.
 * <p/>
 * Example query:
 * <p/>
 * PREFIX edge:   <http://tinkerpop.com/pgm/edge/>
 * PREFIX vertex: <http://tinkerpop.com/pgm/vertex/>
 * PREFIX prop:   <http://tinkerpop.com/pgm/property/>
 * PREFIX pgm:    <http://tinkerpop.com/pgm/ontology#>
 * PREFIX rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
 * <p/>
 * # Select all the userProfile nodes and their name properties...
 * SELECT ?n ?u WHERE {
 *    ?u a pgm:Vertex ;
 *       prop:__ISA__  "userProfile" ;
 *       prop:name     ?n .
 * }
 * <p/>
 * LIMIT 100
 */
@Path("sparql")
public class SparQLResource extends AbstractRestResource {

    private PropertyGraphSail sail;
    private final QueryParser parser = new SPARQLParser();
    private final JsonFactory factory = new JsonFactory();
    private final ObjectMapper mapper = new ObjectMapper(factory);

    /**
     * @param database Injected neo4j database
     */
    public SparQLResource(@Context GraphDatabaseService database, @Context HttpHeaders requestHeaders) {
        super(database, requestHeaders);

    }

    /**
     * Run a sparql query.
     *
     * @param queryString
     * @return
     * @throws Exception
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response sparqlQuery(
            @DefaultValue("") @QueryParam("q") String queryString) throws Exception {
        System.out.println("SparQL Query: " + queryString);

        initSail();
        ParsedQuery query = parser.parseQuery(queryString, "http://ehri-project.eu");
        CloseableIteration<? extends BindingSet, QueryEvaluationException> results
                = sail.getConnection().evaluate(
                query.getTupleExpr(), query.getDataset(), new EmptyBindingSet(), false);
        try {
            List<Map<String, String>> out = Lists.newArrayList();
            while (results.hasNext()) {
                BindingSet next = results.next();
                Map<String, String> set = Maps.newHashMap();
                for (String name : next.getBindingNames()) {
                    set.put(name, next.getValue(name).stringValue());
                }
                out.add(set);
            }
            return Response.ok(mapper.writeValueAsBytes(out)).build();
        } finally {
            results.close();
        }
    }

    /**
     * Initialise the PropertySailGraph
     */
    private void initSail() throws Exception {
        if (sail == null) {
            sail = new PropertyGraphSail(graph.getBaseGraph());
            sail.initialize();
        }
    }

    // TODO: Make me a test
    private void testSparql() throws Exception {
        String queryStr = "PREFIX pgm: <" + PropertyGraphSail.ONTOLOGY_NS + ">\n" +
                "PREFIX prop: <" + PropertyGraphSail.PROPERTY_NS + ">\n" +
                "SELECT ?project ?name WHERE {\n" +
                "   ?marko prop:name \"marko\".\n" +
                "   ?e1 pgm:label \"knows\".\n" +
                "   ?e1 pgm:tail ?marko.\n" +
                "   ?e1 pgm:head ?friend.\n" +
                "   ?e2 pgm:label \"created\".\n" +
                "   ?e2 pgm:tail ?friend.\n" +
                "   ?e2 pgm:head ?project.\n" +
                "   ?project prop:name ?name.\n" +
                "}";

        Graph graph = TinkerGraphFactory.createTinkerGraph();
        Sail sail = new PropertyGraphSail(graph);
        sail.initialize();

        SailRepository repo = new SailRepository(sail);
        SailTupleQuery tupleQuery = repo.getConnection()
                .prepareTupleQuery(QueryLanguage.SPARQL, queryStr, "http://example.org/bogus");
        TupleQueryResult result = tupleQuery.evaluate();

        try {
            while (result.hasNext()) {
                BindingSet next = result.next();
                for (String name : next.getBindingNames()) {
                    System.out.println("key = " + name + ", value = " + next.getValue(name).stringValue());
                }
            }
        } finally {
            result.close();
        }
    }
}
