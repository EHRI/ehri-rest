package eu.ehri.project.cypher;

import eu.ehri.project.utils.LanguageHelpers;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


public class Procedures {

    public class Output {
        public final String value;

        Output(String value) {
            this.value = value;
        }
    }

    public class ListOutput {
        public final List<Object> value;

        ListOutput(List<Object> value) {
            this.value = value;
        }
    }

    @Procedure("eu.ehri.project.cypher.countryCodeToName")
    public Stream<Output> countryCodeToName(@Name("code") String code) {
        return Stream.of(new Output(LanguageHelpers.iso3166dashOneCodeToName(code)));
    }

    @Procedure("eu.ehri.project.cypher.languageCodeToName")
    public Stream<Output> languageCodeToName(@Name("code") String code) {
        return Stream.of(new Output(LanguageHelpers.codeToName(code)));
    }

    @Procedure("coerceList")
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
