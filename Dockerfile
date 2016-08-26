# Docker file for EHRI backend web service
FROM dockerfile/java:oracle-java8

ENV NEO4J_VERSION 3.0.4
ENV NEO4J_HOME /opt/webapps/neo4j-community-$NEO4J_VERSION

# Jax-RS classes we need to configure Neo4j server
ENV JAX_RS_CLASSES dbms.unmanaged_extension_classes=eu.ehri.extension=/ehri

RUN export DEBIAN_FRONTEND=noninteractive && \
  apt-get update && \
  apt-get -y install lsof curl procps && \
  groupadd -r neo4j && \
  useradd -r -g neo4j neo4j && \
  mkdir -p /opt/webapps && \
  curl -0 http://neo4j.com/artifact.php?name=neo4j-community-$NEO4J_VERSION-unix.tar.gz | tar zx -C /opt/webapps && \
  echo $JAX_RS_CLASSES >> $NEO4J_HOME/conf/neo4j.conf && \
  echo dbms.connector.http.address=0.0.0.0:7474  >> $NEO4J_HOME/conf/neo4j.conf && \
  echo dbms.connector.https.address=0.0.0.0:7473 >> $NEO4J_HOME/conf/neo4j.conf && \
  echo dbms.connector.bolt.address=0.0.0.0:7687  >> $NEO4J_HOME/conf/neo4j.conf && \
  echo dbms.security.auth_enabled=false >> $NEO4J_HOME/conf/neo4j.conf && \
  mkdir -p $NEO4J_HOME/scripts

# Copy the output of mvn package to the Neo4j plugin folder...
COPY build/target/ehri-data-*.jar $NEO4J_HOME/plugins

# Copy script for initializing the database...
COPY scripts/lib.sh $NEO4J_HOME/scripts/
COPY scripts/cmd $NEO4J_HOME/scripts/
RUN $NEO4J_HOME/scripts/cmd initialize && \
    $NEO4J_HOME/scripts/cmd gen-schema && \
    chown neo4j.neo4j -R $NEO4J_HOME/data/databases

EXPOSE 7474 7473 7687

CMD $NEO4J_HOME/bin/neo4j start && tail -f $NEO4J_HOME/logs/neo4j.log
