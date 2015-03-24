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

package eu.ehri.project.exceptions;

/**
 * Represents the violation of an integrity constraint
 * during data persistence, typically due to
 * properties that must be unique across the entire
 * graph (e.g. identifiers.)
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IntegrityError extends Exception {
    private String idValue;
    public IntegrityError(String idValue) {
        super("Integrity error for id value: " + idValue);
        this.idValue = idValue;
    }

    public String getIdValue() {
        return idValue;
    }
    private static final long serialVersionUID = -5625119780401587251L;

}
