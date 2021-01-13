# Fragnet

![build and push latest](https://github.com/InformaticsMatters/fragnet/workflows/build%20and%20push%20latest/badge.svg)
![build](https://github.com/InformaticsMatters/fragnet/workflows/build/badge.svg)
![build and push tag](https://github.com/InformaticsMatters/fragnet/workflows/build%20and%20push%20tag/badge.svg)
![build and push latest](https://github.com/InformaticsMatters/fragnet/workflows/build%20and%20push%20stable/badge.svg)

[![Build Status](https://travis-ci.com/InformaticsMatters/fragnet.svg?branch=master)](https://travis-ci.com/InformaticsMatters/fragnet)

![GitHub tag (latest SemVer)](https://img.shields.io/github/tag/informaticsmatters/fragnet)
[![project](https://img.shields.io/badge/Clubhouse%20Project-Fragnet%20Search-5000d2)](https://app.clubhouse.io/informaticsmatters/project/161)

![GitHub](https://img.shields.io/github/license/informaticsmatters/fragnet)

Fragment network tooling from Informatics Matters.
This is primarily Java based tooling using RDKit and the Neo4j graph database
and is the underlying service used by the Fragnet UI application.

Current components:

1. [fragnet-search](fragnet-search/) - fragment network query REST web service.
2. [fragnet-depict](fragnet-depict/) - Depicting smiles as SVG.
3. [orchestration](orchestration/) - legacy orchestration of fragnet-search to AWS.

For more information see:

* [Talk at 2018 RDKit UGM](https://github.com/rdkit/UGM_2018/blob/master/Lightning/tim_dudgeon_fragment-network.pdf).
* [Informatics Matters website](https://www.informaticsmatters.com/pages/fragment_network.html)

## Building (CI)
The project containers are built automatically using GitHub Actions
(see the project workflows in `.github/workflows` for details of the build).
`latest` images pushed to Docker Hub for each change on the
master branch. Tagged releases are also automatically built and pushed to
Docker hub, with `stable` images also produced when the tag is _official_
(i.e. of the form `N.N.N`)

## Building (developer)
Run the buildDockerImages target  to build the container images `./gradlew buildDockerImages`.
This builds two Docker images running Tomcat and deploys the war files
for fragnet-search and fragnet-depict. These container images are `squonk/fragnet-search` and `squonk/fragnet-depict`.
The `squonk/fragnet-search` image contains the Keycloak jars, but authentication needs to be enabled by modifying the
web app.

Test the container using the [docker-compose.yml](docker-compose.yml)
file:

    docker-compose -f docker-compose-test.yml up

This uses a sample Neo4j database with a small amount of data.

## Official deployments
>   For the legacy deployment process (to AWS EC2 instances) refer to 
    the instructions in [orchestration](orchestration/).

New deployments are achieved through the use of Ansible playbooks and Roles
that can be found in the [fragnet-ansible] repository. These are used to
form **Job Templates** that run on our [AWX server].

Official deployments (to Kubernetes) now use AWX.

## Image versions
Official image versions are defined by and obtained from repository tags,
passed on the Travis build process through the `FRAGNET_IMAGE_TAG` environment
variable. This variable over-rides any built-in default present in the
gradle build (see below)...
 
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

---

[awx server]: https://awx.informaticsmatters.org
[fragnet-ansible]: https://github.com/InformaticsMatters/fragnet-ansible
