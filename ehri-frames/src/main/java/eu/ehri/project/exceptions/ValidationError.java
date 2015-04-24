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

import com.google.common.collect.ListMultimap;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.ErrorSet;

/**
 * Represents the failure of incoming data to confirm to
 * some expected format. Like the {@link Bundle} class,
 * errors are held as a sub-tree corresponding to the
 * branches of the incoming data.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ValidationError extends Exception {

    private static final long serialVersionUID = 1L;
    private final ErrorSet errorSet;
    private final Bundle bundle;

    public ValidationError(Bundle bundle, ErrorSet errorSet) {
        this.bundle = bundle;
        this.errorSet = errorSet;
    }

    public ValidationError(Bundle bundle, String key, String error) {        
        this(bundle, ErrorSet.fromError(key, error));
    }

    public ValidationError(Bundle bundle, ListMultimap<String, String> errors) {
        this(bundle, new ErrorSet(errors));
    }

    private static String formatErrors(String clsName, ErrorSet errorSet) {
        return String.format(
                "A validation error occurred building %s: %s%n", clsName, errorSet.toJson());
    }
    
    public String getMessage() {
    	return formatErrors(bundle.getType().getName(), errorSet);
    }

    public ErrorSet getErrorSet() {
        return errorSet;
    }

    public Bundle getBundle() {
        return bundle;
    }
}
