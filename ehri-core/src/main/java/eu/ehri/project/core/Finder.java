/*
 * Copyright 2015 Data Archiving and Networked Services (an institute of
 * Koninklijke Nederlandse Akademie Van Wetenschappen), King's College London,
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

package eu.ehri.project.core;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.ehri.project.models.EntityClass;

import java.util.List;
import java.util.Map;

/**
 * Description of a vertex search query
 */
public class Finder {

    public EntityClass getType() {
        return cls;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    private EntityClass cls;
    private List<Predicate> predicates;

    private Finder(EntityClass cls, List<Predicate> predicates) {
        this.cls = cls;
        this.predicates = predicates;
    }

    public static enum Op {
        STARTS_WITH, ENDS_WITH, EQ, GT, GTE, LT, LTE, NE, CONTAINS;
    }

    public static class Predicate {
        public String property;
        public Op op;
        public Object value;

        public Predicate(String property, Op op, Object value) {
            this.property = property;
            this.op = op;
            this.value = value;
        }
    }

    public static class Builder {
        private EntityClass cls;
        private List<Predicate> predicateList = Lists.newArrayList();

        public Builder(EntityClass cls) {
            this.cls = cls;
        }

        public Builder withPredicate(String property, Op op, Object value) {
            predicateList.add(new Predicate(property, op, value));
            return this;
        }

        public Finder build() {
            return new Finder(cls, predicateList);
        }
    }

    public static Builder newFinder(EntityClass cls) {
        return new Builder(cls);
    }
}
