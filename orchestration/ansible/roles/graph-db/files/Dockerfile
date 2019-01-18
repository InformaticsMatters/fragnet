FROM neo4j:3.5.0

ARG a_version=3.5.0.1
COPY ./docker-entrypoint.sh /
COPY ./graph-algorithms-algo-${a_version}.jar /var/lib/neo4j/plugins/graph-algorithms-algo.jar

RUN chmod 755 /docker-entrypoint.sh && \
    echo 'dbms.security.procedures.unrestricted=algo.*' >> /var/lib/neo4j/conf/neo4j.conf
