# REST Serialization

## Introduction

Since individual REST resources (documentary units, repositories, etc) are comprised of many individual graph nodes, when resources are serialized we serialize their entire "subtree", rather than force the client to fetch each object in parts on a node by node basis. We also allow certain relationships between resources (particularly key "contextual" relationships, to be included in the payload of a resource when fetching its data.

These features make for a much less "chatty" REST interface and one which works on the level of resources proper, rather than the raw nodes and relationships of which they are comprised at the database level. Performing such "convenience" traversals during serialization does, however, involve some tradeoffs and complexities.

## How serialization behavour is specified

Serialization semantics are specified using Java annotations on adjacency methods present on the item domain model interfaces. Domain model interfaces are "frames" for raw graph vertices that provide convenience methods such as "getRepository()". The behaviour for these interface methods (since they are obviously abstract) is provided by [Tinkerpop Frames](https://github.com/tinkerpop/frames/wiki) annotations such as `@Adjacency(label = ..., direction = Direction.IN)` which translate the method call to the desired graph behaviour, by, in this case, performing a graph traversal from a documentary unit node to that of its repository.

### EHRI-specific annotations

On top of the Tinkerpop Frames behaviour, the EHRI persistence layer adds two more annotations which control how framed domain models are serialized (to JSON, or XML, or whatever.) There are:

* `@Fetch`
* `@Dependent`

`@Fetch` , when placed on an interface `get` method also decorated with `@Adjacency` means this relationship should be traversed and items at the other end included in the serialized data. *NB: Only mandatory properties for fetched items are included in the serialized data.*

`@Dependent`, when placed on an interface `get` method also decorated with `@Adjacency` means that that *this* item _belongs to_ the target item. This is mainly used to sanity-check what data gets saved during _de-serialization_, but also affects the behaviour of _serialization_ when deciding which data to include. Dependent items will be serialized with _all_ data _unless_ they belong to an item that is itself being included only in a `@Fetch` traversal.

## Serialization parameters

There are a few parameters which affect the serializer's behaviour.

### Depth

There is a default limit for the number of relationships to traverse when fetching related items. This prevents circular relationships (should they exist) causing infinite recursion, and prevents us sucking too much of the graph.

### 'Lite' mode

This tells the serializer to *only* include mandatory properties for _all_ items (normally this behaviour only applies to items that are `@Fetch`ed.

### Dependent only

This tells the serializer to ignore all `@Fetch` traversals and only include those that are `@Dependent`.

### Overriding Included Properties

Sometimes you want to retrieve an item whilst insuring that a property of a relation is serialized. By default (and the default is currently fixed for the REST interface) only mandatory properties are included in automatically serialized relations. For example, when retrieving the data for a documentary unit, it's repository node is automatically included in the `relationships` data - however only the **mandatory** properties for the repository (it's `identifier` and it's description's `name` and `languageCode`s) are included in order to reduce unnecessary data transfer. We can, however, force tree serialization to include certain properties (which will apply indiscriminately to **all** data types thus serialized.)  These can be specified in the `includeProps` parameter to the serializer, which takes a list of (string) property names to include in the output. From the REST interface, this can be done with the `_ip=<propname>` parameter (which can be given multiple times.)
