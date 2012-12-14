package eu.ehri.project.models.cvoc;

import com.tinkerpop.frames.Property;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.AccessibleEntity;

/**
 * Used for labels, notes and descriptive texts for the CVOC concept. 
 * If it wasn't for the language property this could have simply been a direct concept property. 
 * Coding the language as a property key postfix (like: prefLabel_en-US) 
 * would work but makes querying harder. 
 * Therefore it is a full graph Vertex instead of a property of the Concept Vertex. 
 * 
 * @author paulboon
 *
 */
@EntityType(EntityClass.CVOC_TEXT)
public interface Text extends AccessibleEntity {
	// Note: if we don't need the identifier, permission and history, 
	// we could just extend VertexFrame
    public static final String CONTENT = "content";
    public static final String LANGUAGE = "languageCode";
	
	// textual value or content
    @Property(CONTENT)
    public String getContent();
    @Property(CONTENT)
    public void setContent(String content);

    /** 
     * The language property has the codes for language. 
     * what is the default? Likely it is "en". 
     *  
     * We want to be able to easily convert to SKOS and use standards, so 
     * xml:lang: "This attribute must be set to a language identifier, 
     * as defined by IETF RFC 4646 (http://www.ietf.org/rfc/rfc4646.txt) or successor."
     * 
     * Validation is complex, but wellformdness checks can be simpler. 
     *   [a-z0-9] and minus sign '-' as separator betwen subtags  
     *   and "All subtags have a maximum length of eight characters." 
     *   and case insensitive comparison must be used 
	 *   and the first subtag is the "Primary Language"
     */    
    @Property(LANGUAGE)
    public String getLanguage();
    @Property(LANGUAGE)
    public String setLanguage(String language);
}
