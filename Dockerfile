# Docker file for EHRI backend web service
FROM neo4j:3.0

ENV NEO4J_AUTH=none
ENV NEO4J_dbms_unmanagedExtensionClasses=eu.ehri.extension=/ehri

# Copy the output of mvn package to the Neo4j plugin folder...
COPY build/target/ehri-data*.jar plugins
