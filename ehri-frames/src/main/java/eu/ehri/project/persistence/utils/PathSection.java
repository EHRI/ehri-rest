package eu.ehri.project.persistence.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing one section of a bundle path.
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
        return new PathSection(matcher.group(1), Integer.valueOf(matcher
                .group(2)));
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