package eu.ehri.extension;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.blueprints.oupls.sail.pg.PropertyGraphSail;
import info.aduna.iteration.CloseableIteration;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.neo4j.graphdb.GraphDatabaseService;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.query.parser.sparql.SPARQLParserFactory;
import org.openrdf.repository.sail.SailQuery;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.repository.sail.SailTupleQuery;
import org.openrdf.sail.Sail;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by michaelb on 10/06/13.
 * <p/>
 * Resource for executing SparQL queries on the graph
 * database.
 * <p/>
 * * Insecure and SHOULD NOT be a public endpoint
 * * Ported from Blueprints and Neo4j plugins
 */
@Path("sparql")
public class SparQLResource extends AbstractRestResource {

    private PropertyGraphSail sail;
    private QueryParser parser;
    private SailRepositoryConnection sc;
    private SailRepository repo;

    /**
     * @param database Injected neo4j database
     */
    public SparQLResource(@Context GraphDatabaseService database) {
        super(database);

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response sparqlQuery(
            @DefaultValue("") @QueryParam("q") String queryString) throws Exception {
        System.out.println("SPARQL: " + queryString);
        initSail();
        //test();
        try {

            /*ParsedQuery query = parser.parseQuery(queryString, "http://ehri-project.eu");
            BindingSet bindings = new EmptyBindingSet();
            CloseableIteration<? extends BindingSet, QueryEvaluationException> sparqlResults
                    = sail.getConnection().evaluate(
                    query.getTupleExpr(), query.getDataset(),
                    bindings, false);*/

            /*test2(queryString);


            SailTupleQuery tupleQuery = repo.getConnection().prepareTupleQuery(
                    QueryLanguage.SPARQL, queryString, "http://example.org/bogus");
            TupleQueryResult result = tupleQuery.evaluate();*/

            ParsedQuery query = new SPARQLParser().parseQuery(queryString, "http://example.org/bogus/");
            CloseableIteration<? extends BindingSet, QueryEvaluationException> results
                    = sail.getConnection().evaluate(query.getTupleExpr(), query.getDataset(), new EmptyBindingSet(), false);
            try {
                List<Map<String,String>> out = Lists.newArrayList();
                while (results.hasNext()) {
                    BindingSet next = results.next();
                    Map<String,String> set = Maps.newHashMap();
                    for (String name : next.getBindingNames()) {
                        System.out.println("key = " + name + ", value = " + next.getValue(name).stringValue());
                        set.put(name, next.getValue(name).stringValue());
                    }
                    out.add(set);
                }
                JsonFactory factory = new JsonFactory();
                ObjectMapper mapper = new ObjectMapper(factory);
                TypeReference<List<Map<String,String>>> typeRef = new TypeReference<List<Map<String,String>>>() {
                };

                return Response.ok(mapper.writeValueAsBytes(out)).build();
            } finally {
                results.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e);
        }
    }

    private void test() throws Exception {
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

/*
        System.out.println(queryStr);
        ParsedQuery query = new SPARQLParser().parseQuery(queryStr, "http://example.org/bogus/");
        CloseableIteration<? extends BindingSet, QueryEvaluationException> results
                = sail.getConnection()
                .evaluate(query.getTupleExpr(), query.getDataset(), new EmptyBindingSet(), false);
*/

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

    private void test2(String queryStr) throws Exception {
        ParsedQuery query = new SPARQLParser().parseQuery(queryStr, "http://example.org/bogus/");
        CloseableIteration<? extends BindingSet, QueryEvaluationException> results
                = sail.getConnection().evaluate(query.getTupleExpr(), query.getDataset(), new EmptyBindingSet(), false);
        try {
            while (results.hasNext()) {
                BindingSet set = results.next();
                URI project = (URI) set.getValue("project");
                Literal name = (Literal) set.getValue("name");
                System.out.println("project = " + project + ", name = " + name);
            }
        } finally {
            results.close();
        }
    }


    private void initSail() {
        if (sail == null) {
            sail = new PropertyGraphSail(graph.getBaseGraph());
            //sail = new PropertyGraphSail(TinkerGraphFactory.createTinkerGraph());
            try {
                sail.initialize();
                parser = new SPARQLParserFactory().getParser();
                repo = new SailRepository(sail);
            } catch (Exception e) {
                e.printStackTrace();
                throw new WebApplicationException(e);
            }
        }
    }

    private StreamingOutput streamingResults(final CloseableIteration iterable) throws Exception{
        final ObjectMapper mapper = new ObjectMapper();
        final JsonFactory f = new JsonFactory();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException,
                    WebApplicationException {
                JsonGenerator g = f.createJsonGenerator(arg0);
                g.writeStartArray();

                try {
                    while (iterable.hasNext()) {
                        BindingSet next = (BindingSet)iterable.next();
                        g.writeStartObject();
                        for (String name : next.getBindingNames()) {
                            g.writeFieldName(name);
                            mapper.writeValue(g, next.getValue(name).stringValue());
                        }
                        g.writeEndObject();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                g.writeEndArray();
                g.close();
            }
        };
    }
}
