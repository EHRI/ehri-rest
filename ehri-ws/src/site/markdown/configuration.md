Neo4j 1.9.x Configuration Notes
===============================

 - set extension classes in `conf/neo4j-server.conf` to:

        org.neo4j.server.thirdparty_jaxrs_classes=eu.ehri.extension=/ehri

 - set wrapper user to neo4j

        wrapper.user=neo4j

 - set memory in Neo4j wrapper, adding the line

        wrapper.java.additional=-XX:MaxPermSize=256M

 - for Enterprise version, ensure that conf directory is writable by neo4j users so it can generate its own ssl certs