package eu.ehri.project.definitions;

import com.google.common.collect.Lists;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public interface DefinitionList {

    Boolean isMultiValued();
    String name();

    static ResourceBundle bundle = ResourceBundle.getBundle("eu.ehri.project.definitions.messages");

    static Map<String,String> getMap(DefinitionList[] items, Boolean multivalued) {
        return Lists.newArrayList(items).stream()
                .filter(i -> multivalued == null || i.isMultiValued() == multivalued)
                .map(i -> Lists.newArrayList(i.getName(), i.getDescription()))
                .collect(Collectors.toMap(i -> i.get(0), i -> i.get(1)));
    }

    static Map<String,String> getMap(DefinitionList[] items) {
        return getMap(items, null);
    }

    default String getResourceKey(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    default String messageKey() {
        return String.format("%s.%s", getClass().getSimpleName(), name());
    }

    default String getName() {
        return getResourceKey(messageKey());
    }

    default String getDescription() {
        return getResourceKey(String.format("%s.description", messageKey()));
    }
}
