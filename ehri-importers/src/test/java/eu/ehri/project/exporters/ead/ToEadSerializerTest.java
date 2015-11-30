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

package eu.ehri.project.exporters.ead;

import com.google.common.io.Resources;
import org.junit.Test;
import org.pegdown.LinkRenderer;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.RootNode;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class ToEadSerializerTest {

    @Test
    public void testMarkdownToEad() throws Exception {
        String testMd = Resources.toString(
                Resources.getResource("markdown-sample.md"), StandardCharsets.UTF_8);
        PegDownProcessor processor = new PegDownProcessor();
        RootNode markdown = processor.parseMarkdown(processor
                .prepareSource(testMd.toCharArray()));
        ToEadSerializer eadSerializer = new ToEadSerializer(new LinkRenderer());
        String ead = eadSerializer.toEad(markdown);
        assertTrue(ead.contains("<head>"));
        assertThat(ead, containsString("<head>"));
        assertThat(ead, containsString("<list>"));
        assertThat(ead, containsString("<item>"));
        assertThat(ead, not(containsString("<item><p>")));
        assertThat(ead, containsString("http://portal.ehri-project.eu?foo=bar&amp;bar=baz"));
        assertThat(ead, containsString("extref"));
    }
}