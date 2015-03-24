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

package eu.ehri.project.persistence.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing one section of a bundle path.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
final class PathSection {
    private static final Pattern pattern = Pattern
            .compile("([^/\\[\\]]+)\\[(\\d+|-1)\\]");
    private final String path;
    private final int index;

    private PathSection(String path, int index) {
        this.path = path;
        this.index = index;
    }

    public String getPath() {
        return path;
    }

    public int getIndex() {
        return index;
    }
    
    public static PathSection fromString(String pt) {
        Matcher matcher = pattern.matcher(pt);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Bad path section for nested bundle update: '%s'. "
                                    + "Non-terminal paths should contain relation name and index.",
                            pt));
        }
        return new PathSection(matcher.group(1), Integer.parseInt(
                matcher.group(2)));
    }
    
    @Override
    public int hashCode() {
        return index * 31 + path.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        else if (!(other instanceof PathSection))
            return false;
        else {
            PathSection that = (PathSection)other;
            return path.equals(that.path) && index == that.index;
        }
    }
    

    public String toString() {
        return path + "[" + index + "]";
    }    
}