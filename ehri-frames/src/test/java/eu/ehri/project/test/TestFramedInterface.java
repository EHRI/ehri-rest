package eu.ehri.project.test;

import com.tinkerpop.frames.Property;

public interface TestFramedInterface {

    @Property("array")
    public String[] getArray();
    
    @Property("array")
    public void setArray(final String[] array);
}
