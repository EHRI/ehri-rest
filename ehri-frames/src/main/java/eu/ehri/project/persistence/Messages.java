package eu.ehri.project.persistence;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Messages for persistence classes.
 */
public class Messages {
    private static final String BUNDLE_NAME = "eu.ehri.project.persistence.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    private Messages() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
