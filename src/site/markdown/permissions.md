Permissions
===========

A large amount of the functionality of the EHRI backend concerns setting, enforcing, and validating permissions for what a given user or group of users are allowed to do to certain items in the database. Due to certain inescapable requirements, these functionalities ended up being quite complex. This document is intended to explain how and why the permission system works the way it does. First, some terminology:

* **content type**: A type of first-class "thing" within the DB, e.g. a documentary unit, a repository, a vocabulary, etc. For all intents and purposes, a content type is any type of item that can have permissions attached to it. Each content type has a concrete manifestation in the form of a node within the DB.
* **permission type**: Some way in which a user can interact with an item to somehow affect its representation, e.g. "create", "update", "delete", "annotate". The "owner" permission type is a special case that encompasses "create", "update", "delete", and "annotate".
* **target**: Something which can have permissions attached to it, e.g. a content type item, or the content type node, representing all items of that type.
* **scope**: A content type item which defines the limits to which some permision(s) may be applied (see below).
* **grant**: An assertion that a user can perform an action on a target, with an optional scope.

Use Cases
---------
TODO

Different Grant Types
---------------------
TODO

Permission Scopes
-----------------
There are type-specific behaviours that concern the permission scope, corresponding to how we apply permissions to a given hierarchy. For documentary units the permission scope is the repository at the top level, and the parent doc unit for child items. It does two things:

* It allows you to say that person X can have permission Y on everything which has (directly or by inheritance) a particular "thing" as its scope.
* It determines how IDs are generated for hierarchical entities

When a user wants to do something to a particular item, the permission system traverses "up" the hasPermissionScope hierarchy looking for a permission grant which allows them to do that thing.

For doc units, which form a directed graph, the permission scope is always the parent item or (if top level) the repository. For, e.g. concepts in a vocabulary, the permission scope is always the vocabulary, because they can have multiple "parent" (broader term) items, or none at all.

The permission scope is a generic structure for everything where we want to apply hierarchical permissions. So basically:

* permission scope = de-facto generic representation of a hierarchy
* childOf, inAuthoritativeSet, hasBroaderTerm etc = type-specific hierarchy representations
