#!/bin/bash

# Super-simple script to test the server.
# Assumes your control machine's environment is 'good to go'...

# NOTE: In order to run the tests successfully you need to provide
#       a file in the 'roles/fragnet/vars' directory
#       whose name is 'test_<graph_name>.yaml' that defines
#       the variables 'fragnet_expected_suppliers' and 'availability_tests'.
#       See 'roles/fragnet/vars/test_molport_chemspace_2019_07.yaml' for an example.
#
#       Essentially you have to provide a list of expected suppliers
#       and a list of availability results for named molecules for each
#       supplier.

set -e

ansible-playbook -e '@parameters' playbooks/fragnet/test-fragnet.yaml
