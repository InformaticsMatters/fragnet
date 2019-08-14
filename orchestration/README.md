# Orchestration
This directory contains early Terraform and Ansible files.

You will need: -

- Python 3 (ideally a virtual/conda environment)
- Terraform

## The Squonk Keycloak Server
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

The cluster creation renders the corresponding `ansible/inventory` file
so Ansible playbooks (executed form the `ansible` directory) are ready to run.
 
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
created hosts. You just need to add the `aws_key_name` that's used by
terraform. i.e.:-

    $ eval $(ssh-agent)
    $ ssh-add ~/.ssh/abc-im

## The playbooks
Playbooks are contained in the `playbooks` sub-directory with each play
supported by a corresponding *Role*. There are lots of them but the most
common ones are: -

-   deploy (initial deployment)
-   stop-instances (stops the AWS server instance)
-   start-instances ((re)starts the server instance - after a deployment)

The plays rely on a number of parameters, conveniently replicated
in the `ansible/parameters.template` file. Copy this file as `parameters`
ans edit accordingly for use in the playbooks.

### The 'deploy' playbook
>   **Know your disk requirements before deploying!**
    Before deploying a new graph set you **MUST** make sure there's
    space on the volumes that will host your directories. You will need
    to accommodate the import data, the generated graph data and the
    graph database logs. At the time of writing **molport_build_3**
    required at least 250GiB of file space (25GiB of input data and
    221GiB of generated graph data).

The **deploy** playbook configures the graph-db node with a chosen
_combination_ (or _build_). You define the graph you want to deploy using
a copy of the parameter template file.

**Deploy** essentially ensures the compute instances are _started_,
the graph container is stopped and then everything brought to life
with your chosen graph.

    $ source setenv.sh
    $ cd ansible
    $ cp parameters.template parameters
    [edit your 'parameters' file]
    $ ansible-playbook -e '@parameters' playbooks/fragnet/deploy.yaml 

>   Once you have deployed a database there's no need for you to deploy it
    again. You can simply use the `start-instances` and `stop-instances` plays
    (or handy scripts - see below).

## Testing the (MolPort) graph
You can test the Fragnet service **molport** deployment
using a playbook. The test will attempt to get a token (using the
user credentials in the setenv file), run a built-in search query
(around the molecule `c1ccc(Nc2nc3ccccc3o2)cc1`) and then conclude by checking
the query's results: -

    $ ansible-playbook playbooks/fragnet/test-fragnet.yaml

>   The check simply verifies the number of nodes, edges and groups
    returned by the query. It does not check the values of the nodes and edges,
    getting the right number is enough for this simple test.

## Handy shell-scripts
Super-simple shell-scripts can be used to quickly execute the most common
tasks like `deploy` (a new or existing graph), `stop` (the server) and
`start` (the server): -

    $ ./deploy.sh -e graph_pagecache_size_g=20
    $ ./stop.sh
    $ ./start.sh
    $ ./test.sh
    $ ./restart-search.sh

### Testing via a curl-based REST interaction
On unix, assuming you have `curl` and `jq`, you can
get your token using a `FRAGNET_USERNAME` and `FRAGNET_PASSWORD`
like this...

    $ token=$(curl \
        -d "grant_type=password" \
        -d "client_id=fragnet-search" \
        -d "username=${FRAGNET_USERNAME}" \
        -d "password=${FRAGNET_PASSWORD}" \
        https://squonk.it/auth/realms/squonk/protocol/openid-connect/token 2> /dev/null \
        | jq -r '.access_token')

And then curl the FRAGNET_SERVER...

    $ curl -LH "Authorization: bearer $token" \
        "${FRAGNET_SERVER}/fragnet-search/rest/v2/search/neighbourhood/c1ccc%28Nc2nc3ccccc3o2%29cc1?hac=3&rac=1&hops=2&calcs=LOGP&suppliers=eMolecules-BB"

## Deploying a new database
before deploying, edit your `parameters` file to add a new `graph_set` and then
deploy: -

    [edit your 'parameters' file]
    $ ./deploy.sh

## Adding volumes
Unless the database is very small it is probably wise to create separate
volumes for each one, remembering that there may be volume instance [limits].

For now, add volumes in the AWS console.

-   Add a suitable volume and attach it to the fragnet server
    (which must be running)
-   format (typically a type like `ext4`) the volume
-   mount the volume (see [EBS Volumes])
-   add the volume to fstab for remounts after reboot

Once created and attached this is typically a sequence of commands on the
graph server node like...

    $ lsblk
    $ sudo file -s /dev/<NAME>
    $ sudo mkfs -t ext4 /dev/<NAME>
    $ sudo mkdir /graph-<SET>
    $ sudo mount /dev/<NAME> /graph-<SET>
    
Where `NAME` is the block device name and `SET` is the `graph_set` name you
expect to deploy to this mounted volume.

>   Remember to edit the `fstab` so the volume is mounted when the node is
    restarted (see the AWS [mount] documentation for details on how this is
    done).
    
>   **CRUCIALLY** ... If you ever boot your instance without this volume
    attached (for example, after moving the volume to another instance),
    the `nofail` mount option enables the instance to boot even if there are
    errors mounting the volume. Debian derivatives, including Ubuntu
    versions earlier than 16.04, must also add the `nobootwait` mount option.
    (this is from the AWS [mount] documentation)
    
>   **FURTHER** ... Errors in the `/etc/fstab` file can render a system
    unbootable. Do not shut down a system that has errors in the
    `/etc/fstab` file.

## Example 'in-situ' graph queries
You can access the cypher-shell form within the graph container (assuming
you have the neo4j password): -

    $ docker exec -it <CONTAINTER-ID> bash
    $ export NEO4J_USERNAME=neo4j
    $ export NEO4J_PASSWORD=<PASSWORD>
    $ /var/lib/neo4j/bin/cypher-shell "CALL db.indexes;"
    
Display the known supplier node process IDs and build-times: -

    $ /var/lib/neo4j/bin/cypher-shell \
        "match (s:Supplier) return s.process_id,s.build_datetime;"

Count all nodes and edges: -

    $ /var/lib/neo4j/bin/cypher-shell "match (n) return count(*);"
    $ /var/lib/neo4j/bin/cypher-shell "match (n)-[r]->() return count(r);"

Count supplier/vendor compounds (i.e≥ 'ChemSpace-BB'): -

    $ /var/lib/neo4j/bin/cypher-shell "match (:Available)-->(s:Supplier) \
            where s.name = 'ChemSpace-BB' return count(*);"

Fill the graph cache: -

    $ /var/lib/neo4j/bin/cypher-shell "CALL apoc.warmup.run(true, true, true);"

An example MolPort query for the new (combination 5) DB: -

    $ /var/lib/neo4j/bin/cypher-shell \
        "match (c:MolPort)-->(m:F2)-->(a:Available) \
        where c.smiles = 'CC(=O)NCCOC(=O)C[C@@H](CN)CC(C)C' \
        return c,m,a limit 10;"

---

[ebs volumes]: https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-using-volumes.html
[limits]: https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/volume_limits.html
[mount]: https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-using-volumes.html#ebs-mount-after-reboot
       