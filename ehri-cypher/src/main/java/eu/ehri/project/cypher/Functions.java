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

import com.google.common.base.Joiner;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import eu.ehri.project.IdGeneratorFactory;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.models.idgen.RandomIdGenerator;
import eu.ehri.project.utils.LanguageHelpers;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Functions {

    private static final Config config = ConfigFactory.load();
    private static final RandomIdGenerator idGenerator = IdGeneratorFactory.getIdGenerator();

    @UserFunction
    public String join(@Name("list") Object list, @Name("sep") String sep) {
        return Joiner.on(sep).join(coerceList(list));
    }

    @UserFunction
    public String countryCodeToName(@Name("code") String code) {
        return code == null ? null : LanguageHelpers.countryCodeToName(code);
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

    @UserFunction
    public String ark(@Name("node") Node node) {
        String pid = (String) node.getProperty(Ontology.PID_KEY, null);
        return pid == null ? null : config.getString("io.pids.prefix") + pid;
    }

    @UserFunction
    public String generatePid() {
        return idGenerator.generateId();
    }
}
