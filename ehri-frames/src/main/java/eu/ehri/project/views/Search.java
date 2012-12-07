package eu.ehri.project.views;

import eu.ehri.project.models.base.Accessor;

public interface Search<E> {
    public Iterable<E> list(Accessor user);
    public Search<E> setLimit(Integer limit);
    public Search<E> setOffset(Integer offset);
}
