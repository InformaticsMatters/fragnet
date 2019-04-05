#!/bin/bash

# Super-simple script to stop the server instances.
# Assumes your control machine's environment is 'good to go'...

set -e

ansible-playbook -e '@parameters' playbooks/fragnet/test-fragnet.yaml
