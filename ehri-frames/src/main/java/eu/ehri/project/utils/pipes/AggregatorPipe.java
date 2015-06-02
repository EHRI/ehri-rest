package eu.ehri.project.utils.pipes;

import com.google.common.collect.Lists;
import com.tinkerpop.pipes.AbstractPipe;
import com.tinkerpop.pipes.transform.TransformPipe;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Pipe processor to aggregate item streams into lists given a comparator
 * function that can compare neighbouring items.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class AggregatorPipe<E> extends AbstractPipe<E, List<E>> implements
        TransformPipe<E, List<E>> {

    public interface AggregatorFunction<T> {
        public boolean aggregate(T a, T b, int aggregateCount);
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
