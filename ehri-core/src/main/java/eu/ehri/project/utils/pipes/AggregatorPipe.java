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

package eu.ehri.project.utils.pipes;

import com.google.common.collect.Lists;
import com.tinkerpop.pipes.AbstractPipe;
import com.tinkerpop.pipes.transform.TransformPipe;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Pipe processor to aggregate item streams into lists given a comparator
 * function that can compare neighbouring items.
 */
public class AggregatorPipe<E> extends AbstractPipe<E, List<E>> implements
        TransformPipe<E, List<E>> {

    public interface AggregatorFunction<T> {
        boolean aggregate(T a, T b, int aggregateCount);
    }

    private final AggregatorFunction<E> function;

    public AggregatorPipe(AggregatorFunction<E> function) {
        this.function = function;
    }

    private final List<E> buffer = Lists.newArrayList();

    @Override
    protected List<E> processNextStart() throws NoSuchElementException {
        while (true) {
            try {
                E next = this.starts.next();
                if (buffer.isEmpty()) {
                    buffer.add(next);
                } else {
                    int size = buffer.size();
                    if (function.aggregate(buffer.get(size - 1), next, size)) {
                        buffer.add(next);
                    } else {
                        List<E> copy = Lists.newArrayList(buffer);
                        buffer.clear();
                        buffer.add(next);
                        return copy;
                    }
                }
            } catch (NoSuchElementException e) {
                if (!buffer.isEmpty()) {
                    List<E> copy = Lists.newArrayList(buffer);
                    buffer.clear();
                    return copy;
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void reset() {
        buffer.clear();
        super.reset();
    }
}
