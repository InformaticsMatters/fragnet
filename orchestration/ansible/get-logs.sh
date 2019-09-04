#!/bin/bash

# Super-simple script to get the  fragnet query logs from the server instances.
# Assumes your control machine's environment is 'good to go'...

set -e

ansible-playbook -e '@parameters' playbooks/fragnet/get-logs.yaml
