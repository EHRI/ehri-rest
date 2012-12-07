package eu.ehri.project.models.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A placeholder class for when dummy objects have to return an
 * iterable that contains no items.
 * 
 * @author mike
 *
 * @param <T>
 */
public class EmptyIterable<T> implements Iterable<T>, Iterator<T> {
    public Iterator<T> iterator() {
        return this;
    }
    
    public boolean hasNext() {
        return false;
    }
    
    public T next() {
        throw new NoSuchElementException();
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
