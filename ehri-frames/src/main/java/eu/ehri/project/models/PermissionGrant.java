package eu.ehri.project.models;

import com.tinkerpop.frames.Adjacency;
import eu.ehri.project.models.annotations.EntityType;
import eu.ehri.project.models.annotations.Fetch;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Frame;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;

@EntityType(EntityClass.PERMISSION_GRANT)
public interface PermissionGrant extends Frame {
    public static final String HAS_GRANTEE = "hasGrantee";
    public static final String HAS_SUBJECT = "hasAccessor";
    public static final String HAS_PERMISSION = "hasPermission";
    public static final String HAS_SCOPE = "hasScope";
    public static final String HAS_TARGET = "hasTarget";
    
    @Fetch(value = HAS_SUBJECT, depth=1)
    @Adjacency(label = HAS_SUBJECT)
    public Accessor getSubject();

    @Fetch(value = HAS_GRANTEE, depth=1)
    @Adjacency(label = HAS_GRANTEE)
    public Accessor getGrantee();
    
    @Fetch(value = HAS_TARGET, depth=1)
    @Adjacency(label = HAS_TARGET)
    public Iterable<PermissionGrantTarget> getTargets();

    @Adjacency(label = HAS_TARGET)
    public void addTarget(final PermissionGrantTarget target);

    @Fetch(HAS_PERMISSION)
    @Adjacency(label = HAS_PERMISSION)
    public Permission getPermission();

    @Adjacency(label = HAS_PERMISSION)
    public void setPermission(final Permission permission);

    @Fetch(value = HAS_SCOPE, depth=1)
    @Adjacency(label = HAS_SCOPE)
    public PermissionScope getScope();
    
    @Adjacency(label = HAS_SCOPE)
    public void setScope(final PermissionScope scope);
}
