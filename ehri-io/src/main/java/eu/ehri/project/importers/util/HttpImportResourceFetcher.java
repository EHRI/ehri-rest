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

import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.importers.exceptions.InputParseError;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetch HTTP resources with retry exponential backoff logic.
 */
class HttpImportResourceFetcher {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1_000;
    private static final long MAX_BACKOFF_MS = 30_000;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    public static final int CONNECT_TIMEOUT = 10_000;
    public static final int READ_TIMEOUT = 60_000;

    public void fetch(URL url, ResponseHandler handler) throws IOException, ValidationError, InputParseError {
        IOException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                sleepBeforeRetry(attempt);
            }

            HttpURLConnection connection = null;
            try {
                connection = openConnection(url);
                try (InputStream in = connection.getInputStream()) {
                    handler.handle(in);
                    return; // success — return immediately
                }
            } catch (IOException e) {
                lastException = e;
                if (!isRetryable(e, connection)) {
                    break;  // no point retrying (e.g. 404)
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        throw new IOException("Request failed after " + MAX_RETRIES + " retries", lastException);
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("Accept-Encoding", "gzip");

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            drainErrorStream(connection);  // must drain to allow connection reuse
            throw new HttpStatusException(status, url);
        }
        return connection;
    }

    private boolean isRetryable(IOException e, HttpURLConnection connection) {
        // Always retry network-level errors (timeouts, resets, etc.)
        if (!(e instanceof HttpStatusException)) {
            return true;
        }
        int status = ((HttpStatusException) e).statusCode;
        // Retry on server errors and 429 (rate limited), not client errors
        return status == 429 || (status >= 500 && status < 600);
    }

    private void sleepBeforeRetry(int attempt) throws IOException {
        // Exponential backoff with jitter to avoid thundering herd
        long backoff = (long) (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
        long jitter = (long) (Math.random() * backoff * 0.2);  // ±20% jitter
        long delay = Math.min(backoff + jitter, MAX_BACKOFF_MS);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during retry backoff", ie);
        }
    }

    private void drainErrorStream(HttpURLConnection connection) {
        try (InputStream err = connection.getErrorStream()) {
            if (err != null) {
                byte[] buffer = new byte[4096];
                //noinspection StatementWithEmptyBody
                while (err.read(buffer) != -1) {
                    // discard
                }
            }
        } catch (IOException ignored) {
        }
    }

    @FunctionalInterface
    public interface ResponseHandler {
        void handle(InputStream in) throws IOException, InputParseError, ValidationError;
    }

    public static class HttpStatusException extends IOException {
        private static final long serialVersionUID = -2235809859661477987L;
        public final int statusCode;

        public HttpStatusException(int statusCode, URL url) {
            super("HTTP " + statusCode + " for URL: " + url);
            this.statusCode = statusCode;
        }
    }
}
