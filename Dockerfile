# Docker file for EHRI backend web service
FROM neo4j:4.2.9

ENV NEO4J_HOME=/var/lib/neo4j
ENV NEO4J_AUTH=none
ENV NEO4J_dbms_unmanagedExtensionClasses=eu.ehri.extension=/ehri

# Copy the output of mvn package to the Neo4j plugin folder...
COPY build/target/ehri-data*.jar plugins

# Initialise the database on container start...
COPY scripts/lib.sh scripts/cmd scripts/initdb.sh $NEO4J_HOME/scripts/
ENV EXTENSION_SCRIPT=$NEO4J_HOME/scripts/initdb.sh
