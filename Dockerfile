# Docker file for EHRI backend web service
FROM neo4j:3.2

ENV NEO4J_HOME=/var/lib/neo4j
ENV NEO4J_AUTH=none
ENV NEO4J_dbms_unmanagedExtensionClasses=eu.ehri.extension=/ehri

# Download a logging impl
RUN wget -q -P $NEO4J_HOME/lib \
         http://central.maven.org/maven2/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar && \
    wget -q -P $NEO4J_HOME/lib \
         http://central.maven.org/maven2/ch/qos/logback/logback-core/1.2.3/logback-core-1.2.3.jar

# Copy the output of mvn package to the Neo4j plugin folder...
COPY build/target/ehri-data*.jar plugins

# Initialise the database on container start...
COPY scripts/lib.sh scripts/cmd scripts/initdb.sh $NEO4J_HOME/scripts/
ENV EXTENSION_SCRIPT=$NEO4J_HOME/scripts/initdb.sh
