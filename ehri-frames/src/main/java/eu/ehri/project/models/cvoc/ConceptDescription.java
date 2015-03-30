/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.models.cvoc;

import com.tinkerpop.frames.Property;
import eu.ehri.project.definitions.Ontology;
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
 * @author Paul Boon (http://github.com/PaulBoon)
 *
 */
@EntityType(EntityClass.CVOC_CONCEPT_DESCRIPTION)
public interface ConceptDescription extends Description {

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

    @Property(Ontology.PREFLABEL)
    public String getPrefLabel();
    
// NOTE if it's optional then don't use this Entity interface!
    
//    // More than one!
//    @Property(CONCEPT_ALTLABEL)
//    public String[] getAltLabels();
//    
//    @Property(CONCEPT_DEFINITION)
//    public String getDefinition();
//
//	// NOTE: why has SKOS definitions and scope notes, what is the difference?
//	// scope notes seem to be used in a more flexible way 
//	// and can contain some extra information besides describing the definition. 
//
//    @Property(CONCEPT_SCOPENOTE)
//    public String getScopeNote();

}
