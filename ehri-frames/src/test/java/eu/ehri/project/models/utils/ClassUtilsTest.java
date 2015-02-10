package eu.ehri.project.models.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.DocumentaryUnit;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.test.AbstractFixtureTest;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ClassUtilsTest extends AbstractFixtureTest {

    @Test
    public void testGetEntityType() throws Exception {
        assertEquals(EntityClass.DOCUMENTARY_UNIT,
                ClassUtils.getEntityType(DocumentaryUnit.class));
    }

    @Test
    public void testGetDependentRelations() throws Exception {
        Map<String,Direction> deps = Maps.newHashMap();
        deps.put(Ontology.DESCRIPTION_FOR_ENTITY, Direction.IN);
        assertEquals(deps, ClassUtils.getDependentRelations(DocumentaryUnit.class));
    }

    @Test
    public void testGetFetchMethods() throws Exception {
        Set<String> fetch = Sets.newHashSet(
                Ontology.DESCRIPTION_FOR_ENTITY,
                Ontology.DOC_HELD_BY_REPOSITORY,
                Ontology.DOC_IS_CHILD_OF,
                Ontology.ENTITY_HAS_LIFECYCLE_EVENT,
                Ontology.IS_ACCESSIBLE_TO
        );
        assertEquals(fetch, ClassUtils.getFetchMethods(DocumentaryUnit.class).keySet());
    }

    @Test
    public void testGetPropertyKeys() throws Exception {
        Set<String> keys = Sets.newHashSet(
                Ontology.IDENTIFIER_KEY,
                EntityType.ID_KEY,
                EntityType.TYPE_KEY
        );
        assertEquals(keys, ClassUtils.getPropertyKeys(DocumentaryUnit.class));
    }

    @Test
    public void testGetMandatoryPropertyKeys() throws Exception {
        Set<String> keys = Sets.newHashSet(
                Ontology.IDENTIFIER_KEY
        );
        assertEquals(keys, ClassUtils.getMandatoryPropertyKeys(DocumentaryUnit.class));
    }

    @Test
    public void testGetUniquePropertyKeys() throws Exception {
        Set<String> keys = Sets.newHashSet();
        assertEquals(keys, ClassUtils.getUniquePropertyKeys(DocumentaryUnit.class));
    }
}