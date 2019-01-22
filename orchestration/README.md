# Orchestration
This directory contains early Terraform and Ansible files.

## The Squonk Keycloak Server
The Squonk Keycloak server needs: -

1.  A `fragnet-search` client with a `confidential` **Access Type**
1.  A **Valid Redirect URIs** that is set to the Fragnet Search container
    host and port (until we have a domain re-direction then you'll
    need the cluster IP discussed below)
1.  You will need to put the Keycloak Public Key and client secret
    into your `setenv.sh` (see below)

## Terraform
You can use terraform to create the AWS cluster nodes. You will need: -

- Terraform 0.11.11
- Ansible 2.6.3 or better

To create the cluster (and write the ansible inventory file): -

    $ cd terraform/aws
    $ terraform apply -auto-approve

>   CAUTION: If you're not using S3 for Terraform state then
    you *must* remember that your state files are vital and must not be
    removed while the cluster is active as only you can alter and delete
    the deployed instances and without the state files you'll have to
    delete the instances manually.

The cluster creation renders the corresponding Ansible inventory file
with the created host's details so Ansible is ready to run.
 
To destroy the cluster, return to the Terraform AWS directory and run
the following: -

    $ terraform destroy -force

>   When the cluster has been formed you will (for now) need to transfer the
    IP address of the node (which will be running the (fragnet-search utility)
    to the SKeycloak server.

## Ansible
If you have not used terraform to cerate the cluster you will need to adjust
the inventory file to identify the required hosts for your deployment.
Copy the template as `inventory` and replace the `${}` values.

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

### The playbooks
Playbooks are contained in the `playbooks` sub-directory with each play
supported by a corresponding *Role* task. 

-   deploy
-   stop (the containers)
-   start (the containers)
-   reset (stop and reset the containers - i.e. remove DB)

### The 'deploy' playbook
Configures the graph-db node with a chosen "combination".
The playbook execution for combination "1" would be: -

-   You must have an installation of Ansible
-   You must have AWS credentials defined in suitable environment variables.
    These include: -
    
    -   AWS_ACCESS_KEY_ID
    -   AWS_SECRET_ACCESS_KEY
    -   AWS_DEFAULT_REGION


    $ cd ansible
    $ ansible-playbook -i inventory \
        -e combination=1 \
        -e graph_passqword=blob1234 \
        -e skip_graph=no \
        playbooks/graph-db/deploy.yaml 

>   You can avoid the time-consuming tasks relating to deploying
    the graph database by adding `-e skip_graph=yes`. Using that
    skips deploying the DB, which is different form *not waiting* for
    the database. 

### The 'stop' playbook
Stops the running containers, leaving containers intact.

### The 'start' playbook
Starts the (stopped) containers.

### The 'reset' playbook
Stops the graph database and fragnet search and removes all of its data.
