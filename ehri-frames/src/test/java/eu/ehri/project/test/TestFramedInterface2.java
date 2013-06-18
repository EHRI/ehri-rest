package eu.ehri.project.test;

import com.tinkerpop.frames.Property;

public interface TestFramedInterface2 {

    @Property("foo")
    public String getFoo();
    
    @Property("foo")
    public void setFoo(final String foo);
}
