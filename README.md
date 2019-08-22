# Fragnet

![GitHub](https://img.shields.io/github/license/informaticsmatters/squonk)
[![Build Status](https://travis-ci.org/InformaticsMatters/fragnet.svg?branch=master)](https://travis-ci.org/InformaticsMatters/fragnet)
![GitHub tag (latest SemVer)](https://img.shields.io/github/tag/informaticsmatters/squonk)

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

### Image versions

The application version is defined in `build.gradle`'s `version` string.

During active development the version number (on the master branch)
is typically the next anticipated formal release and **must** have a
`-SNAPSHOT` suffix.

**BEFORE** making and pushing a *formal* release you **must**: -
1.  Remove the `-SNAPSHOT` suffix
1.  Adjust the version number accordingly (if required)
1.  Commit the change to Git
1.  Tag the Git repository using the chosen version number

**AFTER** making a formal release your next actions are to: -
1.  Increment the version number
1.  Append the `-SNAPSHOT` suffix
1.  Commit your change to Git
