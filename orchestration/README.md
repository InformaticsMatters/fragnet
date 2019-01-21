# Orchestration
This directory contains early Terraform and Ansible files.

## Terraform
You can use terraform to create the AWS cluster nodes. You will need: -

- Terraform 0.11.11
- Ansible 2.6.3 or better

To create the cluster (and write the ansible inventory file): -

    $ cd terraform/aws
    $ terraform apply -auto-approve

>   CAUTION: If you're not using S£ for terraform state then
    you must remember that your terraform state files are
    vital and must not be removed as only you can alter and delete
    the deployed instances.
    
## Ansible
If you have not used terraform to cerate the cluster you will need to adjust
the inventory file to identify the required hosts for your deployment.
Copy the template as `inventory` and replace the `${}` values.

### The graph-db playbook
Playbooks are contained in the `playbooks` sub-directory with each play
supported by a corresponding *Role*. 

-   deploy
-   reset

### The deploy playbook
Configures the graph-db node with a "combination" of choice.
The playbook execution for combination "1" would be: -

    $ cd ansible
    $ ansible-playbook -i inventory -e combination=1 playbooks/graph-db/deploy.yaml 

-   You must have an installation of Ansible
-   You must have AWS credentials defined in suitable environment variables.
    These include: -
    
    -   AWS_ACCESS_KEY_ID
    -   AWS_SECRET_ACCESS_KEY
    -   AWS_DEFAULT_REGION

### The reset playbook
Stops the graph database and removes all of its data.
