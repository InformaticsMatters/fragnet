# Orchestration
This directory contains early Terraform and Ansible files.

You will need: -

- Python 3 (ideally a virtual/conda environment)
- Terraform

## The Squonk Keycloak Server
The Squonk Keycloak server needs: -

1.  A `fragnet-search` client with a `confidential` **Access Type**
1.  A **Valid Redirect URIs** that is set to the Fragnet Search container
    host and port (until we have a domain re-direction then you'll
    need the cluster IP discussed below)
1.  The client's *Service Accounts Enabled* and *Direct Access Grants Enabled*
    should be `ON`
1.  You will also need to add a 'fragnet-search' *Role* and users in it
1.  You will need to put the Keycloak Public Key and client secret
    into your `setenv.sh` (see below)

## Python requirements
Ideally work from a **Conda** or Python **Virtual Environment** using the
latest Python 3. From your environment you will need to install
requirements as listed in `orchestration/requirements.txt`: -

    $ cd orchestration
    $ pip install -r requirements.txt
    
## Terraform (hardware provisioning)
To create the cluster (and write the ansible inventory file): -

    $ cd terraform/aws
    $ terraform init
    $ terraform apply -auto-approve

>   Terraform state is preserved in an AWS S3 bucket.

The cluster creation renders the corresponding Ansible inventory file
with the created host's details, so Ansible is ready to run.
 
To destroy the cluster, return to the Terraform AWS directory and run: -

    $ terraform destroy -force

>   When the cluster has been formed you will (for now) need to transfer the
    IP address of the node (which will be running the (fragnet-search utility)
    to the SKeycloak server.

## Ansible (configuration)
>   If you have not used terraform to create the cluster you will need to
    adjust the inventory file to identify the required hosts for your
    deployment. Copy the template as `inventory` and replace the `${}` values.

You will need essential environment variables defined, which Ansible
will expect. Before going any further...

1.  Copy the `setenv-template.sh` as `setenv.sh`
1.  Enter essential environment variable values (you'll need the
    keycloak public ket and client secret discussed above)
1.  *source* it

### The ssh-agent
You might need to use the ssh agent to allow Ansible access to the
created hosts. You just need to add the key used by terraform. i.e. :-

    $ eval $(ssh-agent)
    $ ssh-add ~/.ssh/abc-im

## The playbooks
Playbooks are contained in the `playbooks` sub-directory with each play
supported by a corresponding *Role* task. There are lots of them.

-   deploy
-   stop-instances (the server)
-   start-instances (the server)

The plays rely on a number of parameters, conveniently replicated
in the `parameters.template` file. Copy this file as `parameters`
ans edit accordingly for use in the playbooks.

### The 'deploy' playbook
>   **Know your disk requirements before deploying!**

>   Before deploying a new graph set you **MUST** make sure there's
    space on the volumes that will host your directories. You will need
    to accommodate the import data, the generates graph data and the
    graph database logs. At the time of writing **molport_build_3**
    required at least 250GiB of file space (25GiB of input data and
    221GiB of generated graph data).

The **deploy** playbook configures the graph-db node with a chosen
"combination" (or "build"). You define the graph you want to deploy using
a copy of the parameter template file.

    $ source setenv.sh
    $ cd ansible
    $ cp parameters.template parameters
    [edit your 'parameters' file]
    $ ansible-playbook -e '@parameters' playbooks/fragnet/deploy.yaml 

You can test the Fragnet service **molport** deployment
using a playbook. The test will attempt to get a token (using the
user credentials in the setenv file), run a built-in search query
(around "c1ccc(Nc2nc3ccccc3o2)cc1") and then conclude by checking
an example query's results: -

    $ ansible-playbook playbooks/fragnet/test-fragnet.yaml

>   The check simply verifies the number of nodes, edges and groups
    returned by the query. It does not check the values of the nodes and edges,
    getting the right number is enough for this simple test.

### The 'stop' playbooks
Stops the running containers.

    $ ansible-playbook playbooks/fragnet/stop-containers.yaml 

### The 'start' playbooks
Starts the (stopped) containers.

    $ ansible-playbook playbooks/fragnet/start-containers.yaml 

### The 'undeploy' playbook
Stops the graph database and fragnet search and removes the graph
database (not the import files) and its logs.

Once un-deployed you will need to run the initial `deploy` playbook
to recover the system.

## Handy shell-scripts
Super-simple shell-scripts can be used to quickly execute the most common
tasks like `deploy`, `stop` and `start`: -

    $ ./deploy.sh
    $ ./stop.sh
    $ ./start.sh

## Deploying a new database
You **must** stop the existing containers before you do this. You cannot
deploy a 2nd database while the graph server is serving one. So: -

    $ ansible-playbook playbooks/fragnet/stop-containers.yaml 

>   This is _not_ the stop play, which stops the server.
    This playbook stops the containers.

Then you can edit your `parameters` file to add a new `graph_set` and then
deploy: -

    [edit your 'parameters' file]
    $ ./deploy.sh

>   The database is deployed with the default user (`neo4j`) and password
    (`neo4j`). Before you go any further login to the server at
    `http://${graph_host}:7474` and login and then change the password
    using your chosen NEO4J_PASSWORD so it can be used.
         
## Example REST interaction
Get your token (jq) with a FRAGNET_USERNAME and FRAGNET_PASSWORD ...

    $ token=$(curl \
        -d "grant_type=password" \
        -d "client_id=fragnet-search" \
        -d "username=${FRAGNET_USERNAME}" \
        -d "password=${FRAGNET_PASSWORD}" \
        https://squonk.it/auth/realms/squonk/protocol/openid-connect/token 2> /dev/null \
        | jq -r '.access_token')

And then curl the FRAGNET_HOST...

    $ curl -LH "Authorization: bearer $token" \
        "http://${FRAGNET_HOST}:8080/fragnet-search/rest/v1/search/neighbourhood/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=2&calcs=LOGP,SIM_RDKIT_TANIMOTO"

## Example graph query

    MATCH (c:MolPort)-->(m:F2)-->(a:Available)
        WHERE m.smiles = 'NC1CCCNC1' RETURN c,m,a LIMIT 10
        