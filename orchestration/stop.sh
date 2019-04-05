#!/bin/bash

# Super-simple script to stop the server instances.
# Assumes your control machine's environment is 'good to go'...

set -e

cd ansible; ansible-playbook playbooks/fragnet/stop-instances.yaml; cd ..
