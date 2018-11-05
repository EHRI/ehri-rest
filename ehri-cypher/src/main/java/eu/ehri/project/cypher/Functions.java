package eu.ehri.project.cypher;

import com.google.common.base.Joiner;
import eu.ehri.project.utils.LanguageHelpers;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Functions {

    @UserFunction
    public String join(@Name("list") Object list, @Name("sep") String sep) {
        return Joiner.on(sep).join(coerceList(list));
    }

    @UserFunction
    public String countryCodeToName(@Name("code") String code) {
        return code == null ? null : LanguageHelpers.iso3166dashOneCodeToName(code);
    }

    @UserFunction
    public String languageCodeToName(@Name("code") String code) {
        return code == null ? null : LanguageHelpers.codeToName(code);
    }

    @UserFunction
    public List<Object> coerceList(@Name("data") Object data) {
        if (data == null) {
            return Collections.emptyList();
        } else if (data instanceof List) {
            @SuppressWarnings("unchecked") List<Object> out = (List<Object>) data;
            return out;
        } else if (data instanceof Object[]) {
            return Arrays.asList(((Object[]) data));
        }
        return Collections.singletonList(data);
    }
}
