package eu.ehri.project.cypher;

import eu.ehri.project.utils.LanguageHelpers;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;


public class Procedures {

    public class Output {
        public String value;

        Output(String value) {
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
}
