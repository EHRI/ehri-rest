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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Class representing a path which points to some data in a bundle, i.e.
 * pointing to the startDate attribute of the second hasDate relation of the
 * first node that describes the top-level item would be:
 * <p>
 * describes[0]/hasDate[1]/startDate
 * <p>
 * Everything before the final slash is a PathSection and must indicate the
 * relation name and the index of the desired item.
 */
final class NestableDataPath {

    private static final String PATH_SEP = "/";
    private static final Splitter splitter = Splitter.on(PATH_SEP).omitEmptyStrings();
    private static final Joiner joiner = Joiner.on(PATH_SEP).skipNulls();

    private final List<PathSection> sections;
    private final String terminus;

    private NestableDataPath(List<PathSection> sections, String terminus) {
        Preconditions.checkNotNull(terminus);
        this.sections = ImmutableList.copyOf(sections);
        this.terminus = terminus;
    }

    String getTerminus() {
        return terminus;
    }

    public PathSection current() {
        return sections.get(0);
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }

    public NestableDataPath next() {
        if (sections.isEmpty())
            throw new NoSuchElementException();
        List<PathSection> ns = Lists.newArrayList(sections);
        ns.remove(0);
        return new NestableDataPath(ns, terminus);
    }

    public static NestableDataPath fromString(String path) {
        List<PathSection> sections = Lists.newArrayList();
        List<String> ps = Lists.newArrayList(splitter.split(path));
        String terminus = ps.remove(ps.size() - 1);
        for (String s : ps)
            sections.add(PathSection.fromString(s));
        // If the last part of the path matches a pathsection, i.e.
        // the pattern something[1]
        try {
            sections.add(PathSection.fromString(terminus));
            return new NestableDataPath(sections, null);
        } catch (Exception e) {
            return new NestableDataPath(sections, terminus);
        }
    }

    public String toString() {
        return joiner.join(joiner.join(sections), terminus);
    }
}
