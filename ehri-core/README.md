ehri-core
=========

Classes defining models and handling business logic such as persistence, access control, and permissions validation. 

There are some semantics for managing generic property-graph data that this package uses:

There are two reserved properties for system use:

 * `__id`
 * `__type`

Other properties beginning with two underscores (`__`) are "initialisation properties" and can 
only be set via generic means when the vertex is first created. When a vertex is updated, all
initialisation properties are ignored. When a vertex is serialized to a `Bundle` object, all
initialisation properties are included in the data section (except reserved properties) but
are ignored in object comparison.

Properties beginning with a single underscore are considered metadata and are ignored in object
comparison, but are otherwise handled like regular vertex properties.