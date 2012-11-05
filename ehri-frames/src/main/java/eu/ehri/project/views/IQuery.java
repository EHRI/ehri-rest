package eu.ehri.project.views;

public interface IQuery<E> {
    public Iterable<E> list(Integer offset, Integer limit, Long user);
}
