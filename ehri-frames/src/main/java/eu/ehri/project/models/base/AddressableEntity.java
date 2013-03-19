/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ehri.project.models.base;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.Address;
import eu.ehri.project.models.annotations.Dependent;
import eu.ehri.project.models.annotations.Fetch;

/**
 *
 * @author linda
 */
public interface AddressableEntity {
     public static final String HAS_ADDRESS = "hasAddress";

    @Fetch(HAS_ADDRESS)
    @Dependent
    @Adjacency(label = HAS_ADDRESS)
    public Iterable<Address> getAddresses();

    @Adjacency(label = HAS_ADDRESS)
    public void addAddress(final Address address);
}
