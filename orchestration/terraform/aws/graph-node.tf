# -----------------------------------------------------------------------------
# Master instance definitions
# -----------------------------------------------------------------------------

resource "aws_instance" "graph-node" {
  ami = "${lookup(var.amis, var.aws_region)}"
  instance_type = "r5.xlarge"
  key_name = "${var.aws_key_name}"
  availability_zone = "${var.aws_zone}"
  vpc_security_group_ids = ["${aws_security_group.neo4j.id}",
                            "${aws_security_group.ssh.id}",
                            "${aws_security_group.fragnet.id}"]
  subnet_id = "${var.aws_subnet_id}"
  source_dest_check = false
  disable_api_termination = false
  ebs_optimized = true

  associate_public_ip_address = true

  root_block_device {
    volume_size = "${var.root_volume_size}"
    volume_type = "gp2"
  }

  tags {
    Name = "fragnet"
  }
}

resource "aws_eip_association" "graph_node" {
  instance_id   = "${aws_instance.graph-node.id}"
  allocation_id = "${var.aws_graph_eip_id}"
}
