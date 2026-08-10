/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts,
 * NIOD Institute for War, Holocaust and Genocide Studies (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen).
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.ehri.project.cypher;

import eu.ehri.project.utils.LanguageHelpers;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


public class Procedures {

    public static class Output {
        public final String value;

        Output(String value) {
            this.value = value;
        }
    }

    public static class ListOutput {
        public final List<Object> value;

        ListOutput(List<Object> value) {
            this.value = value;
        }
    }

    @Deprecated
    @Procedure(value = "eu.ehri.project.cypher.countryCodeToName", deprecatedBy = "countryCodeToName")
    public Stream<Output> countryCodeToName(@Name("code") String code) {
        return Stream.of(new Output(LanguageHelpers.countryCodeToName(code)));
    }

    @Deprecated
    @Procedure(value = "eu.ehri.project.cypher.languageCodeToName", deprecatedBy = "languageCodeToName")
    public Stream<Output> languageCodeToName(@Name("code") String code) {
        return Stream.of(new Output(LanguageHelpers.codeToName(code)));
    }

    @Deprecated
    @Procedure(value = "coerceList", deprecatedBy = "coerceList")
    public Stream<ListOutput> toList(@Name("data") Object data) {
        if (data == null) {
            return Stream.of(new ListOutput(Collections.emptyList()));
        } else if (data instanceof List) {
            @SuppressWarnings("unchecked") List<Object> out = (List<Object>) data;
            return Stream.of(new ListOutput(out));
        } else if (data instanceof Object[]) {
            return Stream.of(new ListOutput(Arrays.asList(((Object[]) data))));
        }
        return Stream.of(new ListOutput(Collections.singletonList(data)));
    }
}
