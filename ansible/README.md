# Ansible playbooks
Deployment of Fragnet Search

>   Note: Fragnet Search requires the `anyuid` capability in the
    chosen project's default service account.

The search application is typically deployed using our AWX server GitLab
project but to deploy from a clone of this repository (ideally from
a Python virtual environment) run: -

    $ conda acrivate fragnet
    $ pip install -r requirements.txt
    $ ansible-playbook site-fragnet-search.yaml

Ans to delete it: -

    $ ansible-playbook site-fragnet-search.yaml -e fs_deploy=no

---

[gitlab]: https://gitlab.com/informaticsmatters/ansible-awx
