# Fragnet

Fragment network tooling from Informatics Matters.
This is primarily Java based tooling using RDKit and the Neo4j graph database.

Current components:

1. [fragnet-search](fragnet-search/) - fragment network query REST web service.
2. [fragnet-depict](fragnet-depict/) - Depicting smiles as SVG.
3. [orchestration](orchestration/) - orchestration of fragnet-search to AWS.

For more information see:

* [Talk at 2018 RDKit UGM](https://github.com/rdkit/UGM_2018/blob/master/Lightning/tim_dudgeon_fragment-network.pdf).
* [Informatics Matters website](https://www.informaticsmatters.com/pages/fragment_network.html)

## Building

Run the buildDockerImage target `./gradlew buildDockerImage`.
This builds a Docker image running Tomcat and deploys the war files for fragnet-search and fragnet-depict.

Test the container using the [docker-compose-test.yml](docker-compose-test.yml) file:
`docker-compose -f docker-compose-test.yml up`

This uses a sample Neo4j database with a small amount of data.

For full deployment see [orchestration](orchestration/)