package eu.ehri.project.persistence.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Class representing a path which points to some data in a bundle, i.e.
 * pointing to the startDate attribute of the second hasDate relation of the
 * first node that describes the top-level item would be:
 * 
 * describes[0]/hasDate[1]/startDate
 * 
 * Everything before the final slash is a PathSection and must indicate the
 * relation name and the index of the desired item.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
final class BundlePath {

    public static final String PATH_SEP = "/";
    private static Splitter splitter = Splitter.on(PATH_SEP).omitEmptyStrings();

    private final List<PathSection> sections;
    private final Optional<String> terminus;

    private BundlePath(List<PathSection> sections, Optional<String> terminus) {
        Preconditions.checkNotNull(terminus);
        this.sections = ImmutableList.copyOf(sections);
        this.terminus = terminus;
    }

    public String getTerminus() {
        return terminus.get();
    }
    
    public boolean hasTerminus() {
        return terminus.isPresent();
    }

    public List<PathSection> getSections() {
        return sections;
    }
    
    public PathSection current() {
        return sections.get(0);
    }
    
    public boolean isEmpty() {
        return sections.isEmpty();
    }
    
    public BundlePath next() {
        if (sections.isEmpty())
            throw new NoSuchElementException();
        LinkedList<PathSection> ns = Lists.newLinkedList(sections);
        ns.remove(0);
        return new BundlePath(ns, terminus);
    }

    public static BundlePath fromString(String path) {
        List<PathSection> sections = Lists.newLinkedList();
        List<String> ps = Lists.newArrayList(splitter.split(path));
        String terminus = ps.remove(ps.size() - 1);
        for (String s : ps)
            sections.add(PathSection.fromString(s));
        // If the last part of the path matches a pathsection, i.e.
        // the pattern something[1]
        try {
            sections.add(PathSection.fromString(terminus));
            return new BundlePath(sections, Optional.<String>absent());
        } catch (Exception e) {
            return new BundlePath(sections, Optional.of(terminus));
        }
    }
    
    public String toString() {
        Joiner joiner = Joiner.on("/");
        return joiner.join(joiner.join(sections), terminus.or(""));
    }
}
