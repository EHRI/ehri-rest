package eu.ehri.project.api.impl;

import com.google.common.collect.Sets;
import com.tinkerpop.frames.FramedGraph;
import eu.ehri.project.acl.AclManager;
import eu.ehri.project.acl.ContentTypes;
import eu.ehri.project.acl.GlobalPermissionSet;
import eu.ehri.project.acl.InheritedGlobalPermissionSet;
import eu.ehri.project.acl.InheritedItemPermissionSet;
import eu.ehri.project.acl.PermissionType;
import eu.ehri.project.acl.PermissionUtils;
import eu.ehri.project.acl.SystemScope;
import eu.ehri.project.api.Api;
import eu.ehri.project.api.ConceptsApi;
import eu.ehri.project.api.EventsApi;
import eu.ehri.project.api.QueryApi;
import eu.ehri.project.api.UserProfilesApi;
import eu.ehri.project.api.VirtualUnitsApi;
import eu.ehri.project.core.GraphManager;
import eu.ehri.project.core.GraphManagerFactory;
import eu.ehri.project.definitions.EventTypes;
import eu.ehri.project.definitions.Ontology;
import eu.ehri.project.exceptions.AccessDenied;
import eu.ehri.project.exceptions.DeserializationError;
import eu.ehri.project.exceptions.ItemNotFound;
import eu.ehri.project.exceptions.PermissionDenied;
import eu.ehri.project.exceptions.SerializationError;
import eu.ehri.project.exceptions.ValidationError;
import eu.ehri.project.models.AccessPoint;
import eu.ehri.project.models.AccessPointType;
import eu.ehri.project.models.Annotation;
import eu.ehri.project.models.EntityClass;
import eu.ehri.project.models.Group;
import eu.ehri.project.models.Link;
import eu.ehri.project.models.Permission;
import eu.ehri.project.models.PermissionGrant;
import eu.ehri.project.models.UserProfile;
import eu.ehri.project.models.base.Accessible;
import eu.ehri.project.models.base.Accessor;
import eu.ehri.project.models.base.Actioner;
import eu.ehri.project.models.base.Annotatable;
import eu.ehri.project.models.base.Described;
import eu.ehri.project.models.base.Description;
import eu.ehri.project.models.base.Entity;
import eu.ehri.project.models.base.Linkable;
import eu.ehri.project.models.base.PermissionGrantTarget;
import eu.ehri.project.models.base.PermissionScope;
import eu.ehri.project.models.base.Promotable;
import eu.ehri.project.persistence.ActionManager;
import eu.ehri.project.persistence.Bundle;
import eu.ehri.project.persistence.BundleManager;
import eu.ehri.project.persistence.Mutation;
import eu.ehri.project.persistence.Serializer;
import eu.ehri.project.persistence.VersionManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ApiImpl implements Api {

    private final FramedGraph<?> graph;
    private final Accessor accessor;
    private final PermissionScope scope;
    private final boolean logging;
    private final GraphManager manager;
    private final PermissionUtils helper;
    private final AclManager aclManager;
    private final ActionManager actionManager;
    private final VersionManager versionManager;
    private final VirtualUnitsApiImpl virtualUnitViews;
    private final UserProfilesApiImpl userProfilesApi;
    private final ConceptsApi conceptsApi;
    private final BundleManager bundleManager;
    private final Serializer depSerializer;
    private final EventsApi eventsApi;

    public ApiImpl(FramedGraph<?> graph, Accessor accessor, PermissionScope scope, boolean logging) {
        this.graph = graph;
        this.accessor = accessor;
        this.scope = Optional.ofNullable(scope).orElse(SystemScope.getInstance());
        this.logging = logging;
        this.manager = GraphManagerFactory.getInstance(graph);
        this.helper = new PermissionUtils(graph, scope);
        this.aclManager = new AclManager(graph, scope);
        this.actionManager = new ActionManager(graph, scope);
        this.versionManager = new VersionManager(graph);
        this.virtualUnitViews = new VirtualUnitsApiImpl(graph, accessor);
        this.conceptsApi = new ConceptsApiImpl(graph, accessor, logging);
        this.userProfilesApi = new UserProfilesApiImpl(graph, this);
        this.eventsApi = new EventsApiImpl(graph, accessor);
        this.bundleManager = new BundleManager(graph);
        this.depSerializer = new Serializer.Builder(graph).dependentOnly().build();
    }

    @Override
    public Accessor accessor() {
        return accessor;
    }

    @Override
    public boolean isLogging() {
        return logging;
    }

    @Override
    public ActionManager actionManager() {
        return actionManager;
    }

    @Override
    public VersionManager versionManager() {
        return versionManager;
    }

    @Override
    public VirtualUnitsApi virtualUnits() {
        return virtualUnitViews;
    }

    @Override
    public UserProfilesApi userProfiles() {
        return userProfilesApi;
    }

    @Override
    public ConceptsApi concepts() {
        return conceptsApi;
    }

    @Override
    public EventsApi events() {
        return eventsApi;
    }

    @Override
    public QueryApi query() {
        return new QueryApiImpl(graph, accessor);
    }

    @Override
    public Api withAccessor(Accessor accessor) {
        return new ApiImpl(graph, accessor, scope, logging);
    }

    @Override
    public Api withScope(PermissionScope scope) {
        return new ApiImpl(graph, accessor, Optional.ofNullable(scope)
                .orElse(SystemScope.getInstance()), logging);
    }

    @Override
    public Api enableLogging(boolean logEnabled) {
        return new ApiImpl(graph, accessor, scope, logEnabled);
    }

    @Override
    public <E extends Accessible> E detail(String id, Class<E> cls) throws ItemNotFound {
        E item = manager.getEntity(id, cls);
        if (!aclManager.canAccess(item, accessor)) {
            throw new ItemNotFound(id);
        }
        return item;
    }


    @Override
    public <E extends Accessible> Mutation<E> update(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, ItemNotFound, DeserializationError {
        return update(bundle, cls, Optional.<String>empty());
    }

    @Override
    public <E extends Accessible> E create(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, DeserializationError {
        return create(bundle, cls, Optional.<String>empty());
    }

    @Override
    public <E extends Accessible> Mutation<E> createOrUpdate(Bundle bundle, Class<E> cls)
            throws PermissionDenied, ValidationError, DeserializationError {
        return createOrUpdate(bundle, cls, Optional.<String>empty());
    }

    @Override
    public int delete(String id) throws PermissionDenied, ValidationError, SerializationError, ItemNotFound {
        return delete(id, Optional.<String>empty());
    }

    @Override
    public <E extends Accessible> Mutation<E> update(Bundle bundle, Class<E> cls, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, ItemNotFound, DeserializationError {
        E entity = graph.frame(manager.getVertex(bundle.getId()), cls);
        helper.checkEntityPermission(entity, accessor, PermissionType.UPDATE);
        Mutation<E> out = bundleManager.withScopeIds(scope.idPath()).update(bundle, cls);
        if (logging && out.hasChanged()) {
            actionManager.newEventContext(
                    out.getNode(), accessor.as(Actioner.class),
                    EventTypes.modification, logMessage)
                    .createVersion(out.getNode(), out.getPrior().get())
                    .commit();
        }
        return out;
    }

    @Override
    public <E extends Accessible> E create(Bundle bundle, Class<E> cls, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError {
        helper.checkContentPermission(accessor, helper.getContentTypeEnum(cls),
                PermissionType.CREATE);
        E item = bundleManager.withScopeIds(scope.idPath()).create(bundle, cls);
        // If a user creates an item, grant them OWNER perms on it.
        // Owner permissions do not have a scope.
        // FIXME: Currently a hack here so this doesn't apply to admin
        // users - but it probably should...
        if (!AclManager.belongsToAdmin(accessor)) {
            aclManager.withScope(SystemScope.INSTANCE)
                    .grantPermission(item, PermissionType.OWNER, accessor);
        }
        // If the scope is not the system, set the permission scope
        // of the item too...
        if (!scope.equals(SystemScope.getInstance())) {
            item.setPermissionScope(scope);
        }

        if (logging) {
            actionManager
                    .newEventContext(item, accessor.as(Actioner.class),
                            EventTypes.creation, logMessage)
                    .commit();
        }

        return item;

    }

    @Override
    public <E extends Accessible> Mutation<E> createOrUpdate(Bundle bundle, Class<E> cls, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, DeserializationError {
        helper.checkContentPermission(accessor, helper.getContentTypeEnum(cls),
                PermissionType.CREATE);
        helper.checkContentPermission(accessor, helper.getContentTypeEnum(cls),
                PermissionType.UPDATE);
        Mutation<E> out = bundleManager.withScopeIds(scope.idPath()).createOrUpdate(bundle, cls);
        if (logging && out.updated()) {
            actionManager
                    .newEventContext(out.getNode(), accessor.as(Actioner.class),
                            EventTypes.modification, logMessage)
                    .createVersion(out.getNode(), out.getPrior().get())
                    .commit();
        }
        return out;
    }

    @Override
    public int delete(String id, Optional<String> logMessage)
            throws PermissionDenied, ValidationError, SerializationError, ItemNotFound {
        Accessible item = manager.getEntity(id, Accessible.class);
        helper.checkEntityPermission(item, accessor, PermissionType.DELETE);
        if (logging) {
            actionManager
                    .newEventContext(item, accessor.as(Actioner.class),
                            EventTypes.deletion, logMessage)
                    .createVersion(item)
                    .commit();
        }
        return bundleManager.withScopeIds(scope.idPath())
                .delete(depSerializer.entityToBundle(item));
    }

    @Override
    public AclManager aclManager() {
        return aclManager;
    }

    @Override
    public Acl acl() {
        return new Acl() {
            @Override
            public InheritedGlobalPermissionSet setGlobalPermissionMatrix(Accessor userOrGroup, GlobalPermissionSet permissionSet)
                    throws PermissionDenied {
                checkGrantPermission(accessor, permissionSet);
                aclManager.setPermissionMatrix(userOrGroup, permissionSet);
                boolean scoped = !scope.equals(SystemScope.INSTANCE);
                // Log the action...
                if (logging) {
                    ActionManager.EventContext context = actionManager.newEventContext(
                            userOrGroup.as(Accessible.class),
                            accessor.as(Actioner.class),
                            EventTypes.setGlobalPermissions);
                    if (scoped) {
                        context.addSubjects(scope.as(Accessible.class));
                    }
                    context.commit();
                }
                return aclManager.getInheritedGlobalPermissions(userOrGroup);
            }

            @Override
            public void setAccessors(Accessible entity, Set<Accessor> accessors) throws PermissionDenied {
                helper.checkEntityPermission(entity, accessor, PermissionType.UPDATE);
                aclManager.setAccessors(entity, accessors);
                // Log the action...
                if (logging) {
                    actionManager.newEventContext(
                            entity, accessor.as(Actioner.class), EventTypes.setVisibility)
                            .commit();
                }
            }

            @Override
            public InheritedItemPermissionSet setItemPermissions(Accessible item, Accessor userOrGroup, Set<PermissionType> permissionList)
                    throws PermissionDenied {
                helper.checkEntityPermission(item, accessor, PermissionType.GRANT);
                aclManager.setItemPermissions(item, userOrGroup, permissionList);
                // Log the action...
                if (logging) {
                    actionManager.newEventContext(item,
                            accessor.as(Actioner.class), EventTypes.setItemPermissions)
                            .addSubjects(userOrGroup.as(Accessible.class))
                            .commit();
                }
                return aclManager.getInheritedItemPermissions(item, userOrGroup);
            }

            @Override
            public void revokePermissionGrant(PermissionGrant grant) throws PermissionDenied {
                // TODO: This should be rather more complicated and account for the
                // fact that individual grants can, in theory, have more than one
                // target content type.
                for (PermissionGrantTarget tg : grant.getTargets()) {
                    switch (manager.getEntityClass(tg)) {
                        case CONTENT_TYPE:
                            helper.checkContentPermission(accessor, ContentTypes.withName(tg.getId()), PermissionType.GRANT);
                            break;
                        default:
                            helper.checkEntityPermission(
                                    tg.as(Accessible.class), accessor, PermissionType.GRANT);
                    }
                }
                aclManager.revokePermissionGrant(grant);
            }

            @Override
            public void addAccessorToGroup(Group group, Accessor userOrGroup) throws PermissionDenied {
                ensureCanModifyGroupMembership(group, userOrGroup, accessor);
                group.addMember(userOrGroup);
                // Log the action...
                if (logging) {
                    actionManager.newEventContext(group,
                            accessor.as(Actioner.class), EventTypes.addGroup)
                            .addSubjects(userOrGroup.as(Accessible.class))
                            .commit();
                }
            }

            @Override
            public void removeAccessorFromGroup(Group group, Accessor userOrGroup) throws PermissionDenied {
                ensureCanModifyGroupMembership(group, userOrGroup, accessor);
                group.removeMember(userOrGroup);
                // Log the action...
                if (logging) {
                    actionManager.newEventContext(group,
                            accessor.as(Actioner.class), EventTypes.removeGroup)
                            .addSubjects(userOrGroup.as(Accessible.class))
                            .commit();
                }
            }
        };
    }

    @Override
    public Link createLink(String targetId1, String targetId2, List<String> bodies, Bundle bundle, Collection<Accessor> accessibleTo)
            throws ItemNotFound, ValidationError, PermissionDenied {
        Linkable t1 = manager.getEntity(targetId1, Linkable.class);
        Linkable t2 = manager.getEntity(targetId2, Linkable.class);
        helper.checkEntityPermission(t1, accessor, PermissionType.ANNOTATE);
        // TODO: Should this require perms to link another item???
        //helper.checkEntityPermission(t2, user, PermissionType.ANNOTATE);
        Link link = bundleManager.create(bundle, Link.class);
        link.addLinkTarget(t1);
        link.addLinkTarget(t2);
        link.setLinker(accessor);
        aclManager.setAccessors(link, accessibleTo);
        ActionManager.EventContext eventContext = actionManager.setScope(t1).newEventContext(
                accessor.as(Actioner.class),
                EventTypes.link, Optional.empty())
                .addSubjects(link)
                .addSubjects(t2);
        for (String body : bodies) {
            Accessible item = manager.getEntity(body, Accessible.class);
            link.addLinkBody(item);
            eventContext.addSubjects(item);
        }
        eventContext.commit();
        return link;
    }

    @Override
    public Link createAccessPointLink(String targetId1, String targetId2, String descriptionId, String bodyName,
            AccessPointType bodyType, Bundle bundle, Collection<Accessor> accessibleTo)
            throws ItemNotFound, ValidationError, PermissionDenied {
        Linkable t1 = manager.getEntity(targetId1, Linkable.class);
        Linkable t2 = manager.getEntity(targetId2, Linkable.class);
        Description description = manager.getEntity(descriptionId, Description.class);
        helper.checkEntityPermission(t1, accessor, PermissionType.ANNOTATE);
        // TODO: Should this require perms to link another item???
        //helper.checkEntityPermission(t2, user, PermissionType.ANNOTATE);
        helper.checkEntityPermission(description.getEntity(), accessor, PermissionType.UPDATE);
        Link link = bundleManager.create(bundle, Link.class);
        Bundle relBundle = Bundle.of(EntityClass.ACCESS_POINT)
                .withDataValue(Ontology.NAME_KEY, bodyName)
                .withDataValue(Ontology.ACCESS_POINT_TYPE, bodyType)
                .withDataValue(Ontology.LINK_HAS_DESCRIPTION, link.getDescription());
        AccessPoint rel = bundleManager.create(relBundle, AccessPoint.class);
        description.addAccessPoint(rel);
        link.addLinkTarget(t1);
        link.addLinkTarget(t2);
        link.setLinker(accessor);
        link.addLinkBody(rel);
        aclManager.setAccessors(link, accessibleTo);
        ActionManager.EventContext eventContext = actionManager.newEventContext(
                t1, accessor.as(Actioner.class), EventTypes.link);
        eventContext.addSubjects(link).addSubjects(t2).addSubjects(rel);
        eventContext.commit();
        return link;
    }

    @Override
    public Annotation createAnnotation(String id, String did, Bundle bundle, Collection<Accessor> accessibleTo)
            throws PermissionDenied, AccessDenied, ValidationError, ItemNotFound {
        Annotatable entity = manager.getEntity(id, Annotatable.class);
        Annotatable dep = manager.getEntity(did, Annotatable.class);
        helper.checkEntityPermission(entity.as(Accessible.class), accessor, PermissionType.ANNOTATE);
        helper.checkReadAccess(entity.as(Accessible.class), accessor);

        if (!(entity.equals(dep) || isInSubtree(entity, dep))) {
            // FIXME: Better error message here...
            throw new PermissionDenied("Item is not covered by parent item's permissions");
        }

        Annotation annotation = bundleManager.create(bundle, Annotation.class);
        entity.addAnnotation(annotation);
        if (!entity.equals(dep)) {
            dep.addAnnotationPart(annotation);
        }
        annotation.setAnnotator(accessor.as(UserProfile.class));
        aclManager.setAccessors(annotation, accessibleTo);
        aclManager.withScope(SystemScope.INSTANCE)
                .grantPermission(annotation, PermissionType.OWNER, accessor);

        actionManager.setScope(entity)
                .newEventContext(annotation,
                        accessor.as(Actioner.class),
                        EventTypes.annotation, Optional.empty())
                .commit();
        return annotation;
    }

    @Override
    public Promotable promote(String id) throws ItemNotFound, PermissionDenied, NotPromotableError {
        Promotable item = detail(id, Promotable.class);
        helper.checkEntityPermission(item, accessor, PermissionType.PROMOTE);
        if (!item.isPromotable()) {
            throw new NotPromotableError(item.getId());
        }
        UserProfile user = accessor.as(UserProfile.class);
        item.addPromotion(user);
        if (logging) {
            actionManager.newEventContext(item, user, EventTypes.promotion)
                    .commit();
        }
        return item;
    }

    @Override
    public Promotable removePromotion(String id) throws ItemNotFound, PermissionDenied {
        Promotable item = detail(id, Promotable.class);
        item.removePromotion(accessor.as(UserProfile.class));
        return item;
    }

    @Override
    public Promotable demote(String id) throws ItemNotFound, PermissionDenied, NotPromotableError {
        Promotable item = detail(id, Promotable.class);
        helper.checkEntityPermission(item, accessor, PermissionType.PROMOTE);
        if (!item.isPromotable()) {
            throw new ApiImpl.NotPromotableError(item.getId());
        }
        UserProfile user = accessor.as(UserProfile.class);
        item.addDemotion(user);
        if (logging) {
            actionManager.newEventContext(item, user, EventTypes.demotion)
                    .commit();
        }
        return item;
    }

    @Override
    public Promotable removeDemotion(String id) throws ItemNotFound, PermissionDenied {
        Promotable item = detail(id, Promotable.class);
        item.removeDemotion(accessor.as(UserProfile.class));
        return item;
    }

    @Override
    public int deleteDependent(String parentId, String id, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, SerializationError {
        Described parent = detail(parentId, Described.class);
        Accessible dependentItem = manager.getEntity(id, Accessible.class);
        if (!itemsInSubtree(parent).contains(dependentItem)) {
            throw new PermissionDenied("Given description does not belong to its parent item");
        }
        helper.checkEntityPermission(parent, accessor, PermissionType.UPDATE);
        if (logging) {
            actionManager.newEventContext(parent, accessor.as(Actioner.class),
                    EventTypes.deleteDependent, logMessage)
                    .createVersion(dependentItem)
                    .commit();
        }
        return bundleManager.withScopeIds(parent.idPath())
                .delete(depSerializer.entityToBundle(dependentItem));
    }

    @Override
    public <T extends Accessible> T createDependent(String parentId, Bundle data, Class<T> cls, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, ValidationError {
        Described parent = detail(parentId, Described.class);
        helper.checkEntityPermission(parent, accessor, PermissionType.UPDATE);
        T out = bundleManager.withScopeIds(parent.idPath()).create(data, cls);
        if (logging) {
            actionManager.newEventContext(parent, accessor.as(Actioner.class),
                    EventTypes.createDependent, logMessage)
                    .commit();
        }
        return out;
    }

    @Override
    public <T extends Accessible> Mutation<T> updateDependent(String parentId, Bundle data, Class<T> cls, Optional<String> logMessage)
            throws ItemNotFound, PermissionDenied, ValidationError {
        Described parent = detail(parentId, Described.class);
        helper.checkEntityPermission(parent, accessor, PermissionType.UPDATE);
        Mutation<T> out = bundleManager.withScopeIds(parent.idPath()).update(data, cls);
        if (logging && out.hasChanged()) {
            actionManager
                    .newEventContext(parent, accessor.as(Actioner.class),
                            EventTypes.modifyDependent, logMessage)
                    .createVersion(out.getNode(), out.getPrior().get())
                    .commit();
        }
        return out;
    }


    // Helpers...
    private boolean isInSubtree(Entity parent, Entity child) {
        return itemsInSubtree(parent).contains(child);
    }

    private Set<Entity> itemsInSubtree(Entity topLevel) {
        final Set<Entity> items = Sets.newHashSet();
        depSerializer.traverseSubtree(topLevel, (frame, depth, relation, relationIndex) -> items.add(frame));
        return items;
    }

    /**
     * Check the accessor has GRANT permissions to update another user's
     * permissions.
     *
     * @param accessor      the user
     * @param permissionSet the user's permissions.
     */
    private void checkGrantPermission(Accessor accessor,
            GlobalPermissionSet permissionSet)
            throws PermissionDenied {
        Map<ContentTypes, Collection<PermissionType>> permissionMap
                = permissionSet.asMap();
        // Check we have grant permissions for the requested content types
        if (!AclManager.belongsToAdmin(accessor)) {
            try {
                Permission grantPerm = manager.getEntity(
                        PermissionType.GRANT.getName(), Permission.class);
                for (ContentTypes ctype : permissionMap.keySet()) {
                    if (!aclManager.hasPermission(ctype, PermissionType.GRANT, accessor)) {
                        throw new PermissionDenied(accessor.getId(),
                                ctype.toString(), grantPerm.getId(),
                                scope.getId());
                    }
                }
            } catch (ItemNotFound e) {
                throw new RuntimeException(
                        "Unable to get node for permission type '"
                                + PermissionType.GRANT + "'", e);
            }
        }
    }

    private void ensureCanModifyGroupMembership(Group group, Accessor user, Accessor grantee)
            throws PermissionDenied {
        // If a user is not admin they can only add someone else to a group if
        // a) they belong to that group themselves, and
        // b) they have the modify permission on that group, and
        // c) they have grant permissions for the user
        if (!AclManager.belongsToAdmin(grantee)) {
            boolean found = false;
            for (Accessor acc : grantee.getAllParents()) {
                if (group.equals(acc)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new PermissionDenied(grantee.getId(), group.getId(),
                        "Non-admin users cannot add other users to groups that they" +
                                " do not themselves belong to.");
            }
            helper.checkEntityPermission(user.as(Accessible.class),
                    grantee, PermissionType.GRANT);
            helper.checkEntityPermission(group, grantee, PermissionType.UPDATE);
        }
    }
}
