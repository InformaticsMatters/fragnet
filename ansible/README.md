# Ansible playbooks
Deployment of Fragnet Search

>   Note: Fragnet Search requires the `anyuid` capability in the
    chosen project's default service account.

To deploy: -

    ansible-playbook site-fragnet-search.yaml

To un-deploy: -

    ansible-playbook site-fragnet-search.yaml -e fs_deploy=no

---
