package eu.ehri.project.views;

import eu.ehri.project.models.base.Accessor;

public interface IQuery<E> {
    public Iterable<E> list(Integer offset, Integer limit, Accessor user);
}
