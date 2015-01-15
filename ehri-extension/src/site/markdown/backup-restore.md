Backup and Restore
==================

There are two ways we backup the Neo4j graph:

 - backing up the `graph.db` folder directory
 - doing JSON dumps

Restoring `graph.db` folders is more convenient: you simply replace the current folder with the backed-up one.
Restoring a JSON dump requires running the `GraphSON` command via the `./scripts/cmd` wrapper, e.g:

```
./scripts/cmd graphson -d in /path/to/json/dump/file.json
```

This is best done on a dev machine rather than the server. Note that loading a JSON dump file should be loaded
into a *completely new database*, i.e:

 - copy the .json dump file to your local machine
 - stop any running instances of Neo4j server (if any)
 - set the `$NEO4J_HOME` variable to point to the dir of the Neo4j instance we're loading into
 - from the root of the [ehri-rest](https://github.com/EHRI/ehri-rest) project directory, run 
   `./scripts/cmd -d in db_dump_201402030.json`
   
Note: although the GraphSON loader does buffer input transactions (i.e. it doesn't do the whole load
in one TX) it has been observed to hit a `java.lang.OutOfMemoryError: Java heap space` error during this
operation. If that occurs, increase the `-Xmx1g` arg in the `./scripts/cmd` wrapper to, e.g., 2g.

Once loaded the Solr index should be fully re-built since Neo4j re-uses native graph ids and they
*will have changed*.
