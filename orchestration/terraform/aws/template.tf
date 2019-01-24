# ---------------
# File Templating
# ---------------

data "template_file" "inventory" {
  template = "${file("${var.ansible_dir}/inventory.tpl")}"

  vars {
    graph_host = "${aws_eip_association.graph_node.public_ip}"
  }
}

resource "local_file" "okd_inventory" {
  content = "${data.template_file.inventory.rendered}"
  filename = "${var.ansible_dir}/inventory"
}
