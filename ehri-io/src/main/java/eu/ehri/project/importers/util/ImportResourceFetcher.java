/*
 * Copyright 2026 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie van Wetenschappen), King's College London,
 * Georg-August-Universitaet Goettingen Stiftung Oeffentlichen Rechts
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

package eu.ehri.project.importers.util;

import com.google.common.collect.Maps;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

/**
 * Helper for fetching resources via URL using protocol
 * specific handlers.
 */
public class ImportResourceFetcher {
    private static final HttpImportResourceFetcher httpFetcher = new HttpImportResourceFetcher();

    private interface FetchStrategy {
        void fetch(URL url, HttpImportResourceFetcher.ResponseHandler handler) throws IOException, InputParseError, ValidationError;
    }

    private static class HttpStrategy implements FetchStrategy {

        @Override
        public void fetch(URL url, HttpImportResourceFetcher.ResponseHandler handler) throws IOException, InputParseError, ValidationError {
            httpFetcher.fetch(url, handler);
        }
    }

    private static class FileStrategy implements FetchStrategy {
        @Override
        public void fetch(URL url, HttpImportResourceFetcher.ResponseHandler handler) throws IOException, InputParseError, ValidationError {
            try (InputStream in = url.openStream()) {
                handler.handle(in);
            }
        }
    }

    private static final Map<String, FetchStrategy> STRATEGIES = Maps.newHashMap();

    static {
        HttpStrategy http = new HttpStrategy();
        STRATEGIES.put("http", http);
        STRATEGIES.put("https", http);
        STRATEGIES.put("file", new FileStrategy());
    }

    public void fetch(URL url, HttpImportResourceFetcher.ResponseHandler handler) throws IOException, InputParseError, ValidationError {
        String protocol = url.getProtocol().toLowerCase();
        FetchStrategy strategy = STRATEGIES.get(protocol);
        if (strategy == null) {
            throw new IOException("Unsupported protocol: " + protocol);
        }
        strategy.fetch(url, handler);
    }
}