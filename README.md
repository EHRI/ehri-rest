[![Build Status](https://travis-ci.org/EHRI/ehri-rest.svg?branch=master)](https://travis-ci.org/EHRI/ehri-rest)

The EHRI Data Backend
=====================

A business layer and JAX-RS resource classes for managing [EHRI](http://ehri-project.eu) data.

Integrates with the [Neo4j](http://www.neo4j.org) graph database via a server plugin.

The raison d'être of having a web service backend is to make the job of the front-end easier by 
performing the following functions:

* serialising and deserialising domain-specific object graphs
* handling cascade-delete scenarios for objects that are dependent on one another
* calculating and enforcing access control and action-based permissions on both individual items
  and item-classes in two hierarchical dimensions: user/group roles, and parent-child scopes
* maintaining an audit log of all data-mutating actions, with support for idempotent updates

For documentation (a work-in-progress, but better than nothing) see the docs:

* [All the docs](http://ehri.github.io/docs/api/ehri-rest/index.html)
* [Installing and running from the code repository](http://ehri.github.io/docs/api/ehri-rest/install.html)
* [Importing data](http://ehri.github.io/docs/api/ehri-rest/import.html)
* [Writing management scripts](http://ehri.github.io/docs/api/ehri-rest/scripting.html)
