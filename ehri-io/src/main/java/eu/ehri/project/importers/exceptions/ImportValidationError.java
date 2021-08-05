/*
 * Copyright 2020 Data Archiving and Networked Services (an institute of
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

package eu.ehri.project.importers.exceptions;

import eu.ehri.project.exceptions.ValidationError;

/**
 * The input data supplied was invalid in some way, detailed by @param cause.
 */
public class ImportValidationError extends Exception {
    private static final long serialVersionUID = -5295648478089620968L;

    private final String context;
    private final ValidationError cause;
    /**
     * Constructor.
     *
     * @param context a description of the error
     * @param cause the root exception
     */
    public ImportValidationError(String context, ValidationError cause) {
        super(context, cause);
        this.context = context;
        this.cause = cause;
    }

    public ValidationError getError() {
        return cause;
    }

    public String getContext() {
        return context;
    }
}
