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

package eu.ehri.project.importers.exceptions;

/**
 * The input data supplied was invalid in some way, detailed by @param cause.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InputParseError extends Exception {
    private static final long serialVersionUID = -5295648478089620968L;

    /**
     * Constructor.
     *
     * @param cause the root exception
     */
    public InputParseError(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message a description of the error
     */
    public InputParseError(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message a description of the error
     * @param cause the root exception
     */
    public InputParseError(String message, Throwable cause) {
        super(message, cause);
    }
}
