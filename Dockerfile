# Docker file for EHRI backend web service
FROM dockerfile/java:oracle-java8

ENV NEO4J_VERSION 2.3.2
ENV NEO4J_HOME /opt/webapps/neo4j-community-$NEO4J_VERSION

# Jax-RS classes we need to configure Neo4j server
ENV JAX_RS_CLASSES org.neo4j.server.thirdparty_jaxrs_classes=eu.ehri.extension=/ehri

# The name of local data output from our Maven build
ENV EHRI_PLUGIN assembly-0.1.tar.gz

# http://neo4j.com/artifact.php?name=neo4j-community-1.9.9-unix.tar.gz

RUN export DEBIAN_FRONTEND=noninteractive && \
  apt-get update && \
  apt-get -y install lsof curl procps && \
  groupadd -r neo4j && \
  useradd -r -g neo4j neo4j && \
  mkdir -p /opt/webapps && \
  curl -0 http://neo4j.com/artifact.php?name=neo4j-community-$NEO4J_VERSION-unix.tar.gz | tar zx -C /opt/webapps && \
  echo $JAX_RS_CLASSES >> $NEO4J_HOME/conf/neo4j-server.properties && \
  echo org.neo4j.server.webserver.address=0.0.0.0 >> $NEO4J_HOME/conf/neo4j-server.properties && \
  perl -pi -e 's/dbms\.security\.auth_enabled=true/dbms.security.auth_enabled=false/' $NEO4J_HOME/conf/neo4j-server.properties && \
  $NEO4J_HOME/bin/neo4j-installer install && \
  mkdir -p $NEO4J_HOME/plugins/ehri && \
  mkdir -p $NEO4J_HOME/scripts

# Copy the output of mvn package to the Neo4j plugin folder...
COPY assembly/target/$EHRI_PLUGIN /tmp/
RUN tar zxf /tmp/$EHRI_PLUGIN -C $NEO4J_HOME/plugins/ehri && \
    rm /tmp/$EHRI_PLUGIN

# Copy script for initializing the database...
COPY scripts/lib.sh $NEO4J_HOME/scripts/
COPY scripts/cmd $NEO4J_HOME/scripts/
RUN $NEO4J_HOME/scripts/cmd initialize
RUN chown neo4j.neo4j -R $NEO4J_HOME/data

EXPOSE 7474

CMD service neo4j-service start-no-wait && tail -f $NEO4J_HOME/data/log/console.log
