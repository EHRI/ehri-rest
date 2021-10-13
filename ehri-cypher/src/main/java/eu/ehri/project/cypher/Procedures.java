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
        return Stream.of(new Output(LanguageHelpers.iso3166dashOneCodeToName(code)));
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
