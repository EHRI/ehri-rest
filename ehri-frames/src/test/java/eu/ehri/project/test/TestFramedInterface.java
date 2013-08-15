package eu.ehri.project.test;

import com.tinkerpop.frames.Property;

import java.util.List;

public interface TestFramedInterface {

    @Property("list")
    public List<String> getList();
    
    @Property("list")
    public void setList(final List<String> list);
}
