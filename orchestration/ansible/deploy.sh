#!/bin/bash

# Super-simple script to deploy a graph and start the server instances.
# Assumes your control machine's environment is 'good to go'...

set -e

ansible-playbook -e '@parameters' playbooks/fragnet/deploy.yaml
