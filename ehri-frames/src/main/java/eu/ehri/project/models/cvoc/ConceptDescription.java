package eu.ehri.project.models.cvoc;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.base.Description;

/**
 * labels, notes and texts that describe the concept, for a specific language 
 * are combined into this description
 * 
 * NOTE: we should only allow one prefLabel (and thus one ConceptDescription) per language, 
 * but we cannot model that
 * 
 * @author paulboon
 *
 */
@EntityType(EntityClass.CVOC_CONCEPT_DESCRIPTION)
public interface ConceptDescription extends Description {
    public static final String LANGUAGE = Description.LANGUAGE_CODE; //"languageCode";
    public static final String PREFLABEL = Description.NAME; //"prefLabel";
    public static final String ALTLABEL = "altLabel";
    public static final String DEFINITION = "definition";
    public static final String SCOPENOTE = "scopeNote";

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
// NOTE already in Description
//    @Property(LANGUAGE)
//    public String getLanguage();
//    @Property(LANGUAGE)
//    public String setLanguage(String language);
        

    // Note: maybe we could make a OptionalProperty annotation?

    @Property(PREFLABEL)
    public String getPrefLabel();
    
// NOTE if it's optional then don't use this Entity interface!
    
//    // More than one!
//    @Property(ALTLABEL)
//    public String[] getAltLabels();
//    
//    @Property(DEFINITION)
//    public String getDefinition();
//
//	// NOTE: why has SKOS definitions and scope notes, what is the difference?
//	// scope notes seem to be used in a more flexible way 
//	// and can contain some extra information besides describing the definition. 
//
//    @Property(SCOPENOTE)
//    public String getScopeNote();

}
