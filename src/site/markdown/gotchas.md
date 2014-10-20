Miscellaneous Gotchas
=====================

## Online Backup Fails with BlockingReadTimeoutException

```
Couldn't connect to 'single://localhost:6362'
org.neo4j.com.ComException: org.jboss.netty.handler.queue.BlockingReadTimeoutException
``` 

This has been observed to occur when the database has had some offline operations applied
to it, i.e. an bulk data import. The server appears to otherwise function without problems.
The error seemingly went away when the database was written to in some (trivial) way, i.e.
watching/unwatching an item.

## Mysterious ItemNotFound exceptions in unexpected places, i.e. importing data

It has been known on some very rare occasions for unexpected ItemNotFound exceptions to
be thrown in places where that doesn't seem to make sense, i.e. the API can't find data
it's just created. The cause of this seems to be the Neo4j index somehow getting corrupted,
which means that item index lookups fail. The way to fix this is to rebuild the graph index
via the `reindex` command. First, make sure the Neo4j server is stopped:

```
./neo4j-community-1.9.2/bin/neo4j stop
```

Then, ensuring that the `$NEO4J_HOME` var is correctly set to point to your database, run:

```
./scripts/cmd reindex
```

This should only take a couple of seconds. It will first delete the global graph index (name
"entities"), create it again, and then iterate over every node adding its mandatory properties.

The precise cause of this index corruption is not known, and it's only been witnessed on very
specific Apple Macs (i.e. one dev's machine.)
