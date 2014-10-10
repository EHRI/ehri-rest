Miscellaneous Gotchas
=====================

Online Backup Fails with BlockingReadTimeoutException
----------------------------------------------------- 

```
Couldn't connect to 'single://localhost:6362'
org.neo4j.com.ComException: org.jboss.netty.handler.queue.BlockingReadTimeoutException
``` 

This has been observed to occur when the database has had some offline operations applied
to it, i.e. an bulk data import. The server appears to otherwise function without problems.
The error seemingly went away when the database was written to in some (trivial) way, i.e.
watching/unwatching an item. 
