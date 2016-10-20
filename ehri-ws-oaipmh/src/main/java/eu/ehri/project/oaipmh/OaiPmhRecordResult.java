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

package eu.ehri.project.oaipmh;

import eu.ehri.project.models.DocumentaryUnit;

import java.util.Optional;

class OaiPmhRecordResult {

    private final DocumentaryUnit doc;
    private final OaiPmhDeleted deleted;

    private OaiPmhRecordResult(OaiPmhDeleted deleted, DocumentaryUnit doc) {
        this.doc = doc;
        this.deleted = deleted;
    }

    static OaiPmhRecordResult of(DocumentaryUnit doc) {
        return new OaiPmhRecordResult(null, doc);
    }

    static OaiPmhRecordResult deleted(OaiPmhDeleted deleted) {
        return new OaiPmhRecordResult(deleted, null);
    }

    static OaiPmhRecordResult invalid() {
        return new OaiPmhRecordResult(null, null);
    }

    Optional<DocumentaryUnit> doc() {
        return Optional.ofNullable(doc);
    }

    Optional<OaiPmhDeleted> deleted() {
        return Optional.ofNullable(deleted);
    }

    boolean isInvalid() {
        return deleted == null && doc == null;
    }
}
