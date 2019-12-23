Running data extraction queries for SAMPL7.

These queries are quick hacked together code to extract data.
This is not intend to be part of the main codebase.

Build the docker image with:

./gradlew dockerBuildImage

docker push tdudgeon/fragnetexpand:latest

Either run the container directly (Main class is specified in the mainClassName property in the fragnet-search/build.gradle file):

docker run -t -e NEO4J_SERVER=<server> -e NEO4J_PASSWORD=<password> -e LIMIT=100 tdudgeon/fragnetexpand:latest

Create pod using:

apiVersion: v1
kind: Pod
metadata:
  name: fragnetexpand
spec:
  containers:
  - name: fragnetexpand
    image: tdudgeon/fragnetexpand:latest
    # Just spin & wait forever
    command: [ "/bin/bash", "-c", "--" ]
    args: [ "while true; do sleep 30; done;" ]

Or rsh into the container if on OpenShift and run like this:


export NEO4J_SERVER=<server>
export NEO4J_PORT=7687
export NEO4J_PASSWORD=<password>
export INPUT=/tmp/input.smi
export HAC=3
export RAC=1
export HOPS=1

java -cp /app/resources:/app/classes:/app/libs/* org.squonk.fragnet.search.queries.examples.ExpandHits > /tmp/output-hops${HOPS}-hac${HAC}-rac${RAC}.txt


To generate the 53 hits file run this within the container:


cat << EOF > /tmp/input.smi
CC1=NNC=C1CNC=2C=CC=CC2F
FC=1C=CC(=CC1)N2CCN(CC2)C(=O)C3CC3
COC(=O)NC=1C=CC(CC=2C=CN=CC2)=CC1
CS(=O)(=O)NCCC=1C=CC(F)=CC1
CN1CCN(CC1)C(=O)COC=2C=CC(C)=CC2
C[C@@H](CO)N(C)C=1N=CC=CC1F
CC1=NC=2C=CC=CC2N1CCC(=O)N
COC=1C=CC=2N=C(NC(=O)C3CC3)SC2C1
COC=1C=C(Br)C=CC1CO
NC(=O)N1CCN(CC1)C(=O)C2=CC=CO2
CNC(=O)C1(CC=2C=CC=CC2F)CC1
CC=1C=C(CN2N=C(C)C=CC2=O)ON1
CC=1C=CSC1C(=O)O
CC(=O)NCC=1C=NNC1
FC=1C=CC=CC1NCC=2C=CNN2
O=C(NC1CC1)C=2C=CC=3OCOC3C2
CC1=CC=C(N)C(=O)N1C
CN(CCO)C=1C=CC=CN1
CCNC(=O)C1=CNN=N1
NC(=O)N1CCN(CC1)C=2C=CC(F)=CC2
CN(C)C(=O)C=1C=NNC1
CC(O)C=1C=NN(C1)C2CCCC2
CCOC=1C=CC(CC(=O)O)=CC1
COC=1C=CC=C2NC(=O)CCCC12
O=C(N1CCCC1)C=2C=NNC2
COCC1=NN=C(N)S1
CC=1C=C(F)C=C(C1)S(=O)(=O)N
CC(=O)NCC=1C=CC(=CC1)S(=O)(=O)N
CS(=O)(=O)CC1=NC=2C=CC=CC2N1
COCCN1C=C(Br)C=N1
CCOC(=O)N1CCOCC1
CNC=1C=CC=CC1S(=O)(=O)C
OCCN1C=C(Br)C=N1
CN1N=NC=2CCC(CC21)C(=O)O
CCN1C=C(CCO)C=N1
CNCC1=NN=C2C=CC=CN12
CCN(CC)C1=CC(C)=NC2=NC=NN12
CC(C)C=1N=CC(Cl)=C(N1)C(=O)N
OC1CN(C1)C(C=2C=CC=CC2)C=3C=CC=CC3
BrC=1C=CC=2CC(=O)NC2C1
OC=1C=CC(Br)=CC1C#N
CCOC(=O)C=1C=NNC1
C[C@@H](CO)NC=1N=CC(Cl)=CC1F
CCN1C=C(NC(=O)C2CCCC2)C=N1
OCCN1C=CC(Br)=CC1=O
CC(=O)NCCCCC(N)C(=O)O
CCN1C=C(NC(=O)C2CCC2)C=N1
O=C(NC1CC1)C=2C=NN3C=CC=NC23
COC=1C=C(Br)C=CC1CC(=O)O
CCN1C=C(CNC(=O)C=2C=CC=NC2)C=N1
CC1=CC(=NO1)C(=O)NC=2C=NN(C)C2
ClC=1C=CN=C2N=CC=CC12
O=C(CC1CCCCC1)NN2C=NN=C2
EOF