---

all:
  vars:
    fragnet_server_id: ${instance_id}
    fragnet_server_region: ${instance_region}

  children:
    graph:
      hosts:
        ${graph_host}:
          ansible_user: centos
