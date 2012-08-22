package eu.ehri.project.models;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface Address {
    
    @Adjacency(label=Agent.HAS_ADDRESS, direction=Direction.IN)
    public Agent getAgent();

    @Property("streetAddress")
    public String getStreetAddress();
    
    @Property("streetAddress")
    public void setStreetAddress(String streetAddress);
    
    @Property("region")
    public String getRegion();
    
    @Property("region")
    public void setRegion(String region);
    
    @Property("city")
    public String getCity();
    
    @Property("city")
    public void setCity(String city);
    
    @Property("countryCode")
    public String getCountryCode();
    
    @Property("countryCode")
    public void setCountryCode(String countryCode);
    
    @Property("telephone")
    public String getTelephone();
    
    @Property("telephone")
    public void setTelephone(String telephone);
    
    @Property("fax")
    public String getFax();
    
    @Property("fax")
    public void setFax(String fax);
    
    @Property("email")
    public String getEmail();
    
    @Property("email")
    public void setEmail(String email);
    
    @Property("url")
    public String getUrl();
    
    @Property("url")
    public void setUrl(String url);
    
}
