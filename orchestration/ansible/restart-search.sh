#!/bin/bash

# Super-simple script to stop and then re-start the fragnet search container.
# Assumes your control machine's environment is 'good to go'
# and the instances have are started (via `start.sh`)

set -e

ansible-playbook playbooks/fragnet/stop-fragnet-search.yaml
ansible-playbook playbooks/fragnet/start-fragnet-search.yaml
