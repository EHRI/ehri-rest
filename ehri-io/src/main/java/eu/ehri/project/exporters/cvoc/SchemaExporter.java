package eu.ehri.project.exporters.cvoc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.InverseOf;
import eu.ehri.project.models.annotations.Mandatory;
import eu.ehri.project.models.utils.ClassUtils;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dump an RDF schema + OWL representation of the model schema.
 */
public class SchemaExporter {

    private final String rdfFormat;

    public SchemaExporter(String rdfFormat) {
        this.rdfFormat = rdfFormat;
    }

    public final static String DEFAULT_BASE_URI = "http://data.ehri-project.eu/ontology/";
    public final static Map<String, String> NAMESPACES = ImmutableMap.<String, String>builder()
            .put("owl", OWL.getURI())
            .put("xsd", XSD.getURI())
            .put("rdfs", RDFS.getURI())
            .put("ehri", DEFAULT_BASE_URI)
            .build();

    public void dumpSchema(OutputStream outputStream, String baseUri) throws UnsupportedEncodingException {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(NAMESPACES);

        Set<Class<?>> baseClasses = Sets.newHashSet();

        for (EntityClass entityClass : EntityClass.values()) {
            Collections.addAll(baseClasses, entityClass.getJavaClass().getInterfaces());
        }

        for (Class<?> sup : baseClasses) {
            dumpEntityClass(model, sup);
        }

        for (EntityClass entityClass : EntityClass.values()) {
            Class<?> cls = entityClass.getJavaClass();
            Resource shape = dumpEntityClass(model, cls);

            for (Class<?> sup : cls.getInterfaces()) {
                model.add(shape, RDFS.subClassOf,
                        model.createResource(DEFAULT_BASE_URI + sup.getSimpleName()));
            }
        }

        model.getWriter(rdfFormat).write(model, outputStream, baseUri);
    }

    // Helpers
    private XSDDatatype typeOf(Class<?> cls) {
        if (Number.class.isAssignableFrom(cls)) {
            return XSDDatatype.XSDinteger;
        } else if (Boolean.class.isAssignableFrom(cls)) {
            return XSDDatatype.XSDboolean;
        } else {
            return XSDDatatype.XSDstring;
        }
    }

    private Resource dumpEntityClass(Model model, Class<?> cls) {
        String name = cls.getSimpleName();

        // Create the rdfClass...
        Resource rdfClass = model.createResource(DEFAULT_BASE_URI + name);
        model.add(rdfClass, RDF.type, OWL.Class);
        model.add(rdfClass, RDFS.label, name);

        for (Method method : cls.getDeclaredMethods()) {
            Property property = method.getAnnotation(Property.class);
            Class<?> returnType = method.getReturnType();

            if (property != null) {
                String propertyName = property.value();
                boolean mandatory = method.getAnnotation(Mandatory.class) != null;
                addDatatypeProperty(model, rdfClass, propertyName, returnType, mandatory);
            } else {
                Adjacency adjacency = method.getAnnotation(Adjacency.class);
                InverseOf inverseOf = method.getAnnotation(InverseOf.class);
                if (adjacency != null
                        && method.getName().startsWith(ClassUtils.FETCH_METHOD_PREFIX)
                        && (adjacency.direction().equals(Direction.OUT) || inverseOf != null)) {
                    boolean mandatory = method.getAnnotation(Mandatory.class) != null;
                    String label = inverseOf != null ? inverseOf.value() : adjacency.label();
                    addObjectProperty(model, rdfClass, method, returnType, label, mandatory);
                }
            }
        }

        return rdfClass;
    }

    private void addObjectProperty(Model model, Resource rdfClass, Method method,
            Class<?> returnType, String name, boolean mandatory) {
        boolean collection = Iterable.class.isAssignableFrom(returnType);
        if (collection) {
            // This is (probably) an iterable frame rel
            ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
            returnType = (Class<?>) type.getActualTypeArguments()[0];
            mandatory = false;
        }

        Resource propResource = model.createResource(DEFAULT_BASE_URI + name);
        model.add(propResource, RDF.type, OWL.ObjectProperty);
        model.add(propResource, RDFS.domain, rdfClass);
        model.add(propResource, RDFS.range,
                model.createResource(DEFAULT_BASE_URI + returnType.getSimpleName()));

        Resource restriction = model.createResource();
        model.add(restriction, RDF.type, OWL.Restriction);
        model.add(restriction, OWL.onProperty, propResource);
        model.add(restriction, OWL.allValuesFrom,
                model.createResource(DEFAULT_BASE_URI + returnType.getSimpleName()));
        if (mandatory) {
            model.add(restriction, OWL.cardinality,
                    model.createTypedLiteral(1, XSDDatatype.XSDnonNegativeInteger));
        }
        model.add(rdfClass, RDFS.subClassOf, restriction);
    }

    private void addDatatypeProperty(Model model, Resource rdfClass,
            String propertyName, Class<?> type, boolean mandatory) {
        Resource propResource = model.createResource(DEFAULT_BASE_URI + propertyName);
        model.add(propResource, RDF.type, OWL.DatatypeProperty);

        model.add(propResource, RDFS.domain, rdfClass);
        model.add(propResource, RDFS.range,
                model.createResource(typeOf(type).getURI()));

        Resource restriction = model.createResource();
        model.add(restriction, RDF.type, OWL.Restriction);
        model.add(restriction, OWL.onProperty, propResource);
        model.add(restriction, mandatory ? OWL.cardinality : OWL.maxCardinality,
                model.createTypedLiteral(1, XSDDatatype.XSDnonNegativeInteger));
        model.add(rdfClass, RDFS.subClassOf, restriction);

        if (type.isEnum()) {
            List<RDFNode> inValues = Lists.newArrayList();
            for (Object constValue : type.getEnumConstants()) {
                inValues.add(model.createLiteral(constValue.toString()));
            }
            model.add(propResource, OWL.oneOf, model.createList(inValues.iterator()));
        }
    }
}
