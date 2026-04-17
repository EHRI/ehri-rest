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

package eu.ehri.project.models.idgen;

import java.security.SecureRandom;

/**
 * The Archival Resource Key ID generator uses a limited alphabet to avoid
 * ambiguous characters such as o, 0, i, 1, and l. It always starts with a
 * letter, with the remainder being alphanumeric.
 */
public class ArkIdGenerator implements RandomIdGenerator {
    public static final String HEAD_ALPHABET = "abcdefghjkmnpqrstuvwxyz";
    public static final String ALPHABET = HEAD_ALPHABET + "23456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final int length;

    public ArkIdGenerator(int length) {
        this.length = length;
    }

    @Override
    public String generateId() {
        StringBuilder sb = new StringBuilder(length);
        sb.append(HEAD_ALPHABET.charAt(RANDOM.nextInt(HEAD_ALPHABET.length())));

        for (int i = 1; i < length; i++) {
            // Pick a random index from 0 to alphabet.length() - 1
            int index = RANDOM.nextInt(ALPHABET.length());

            // Add the character at that index to our string
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }
}
