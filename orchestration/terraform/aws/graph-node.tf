# -----------------------------------------------------------------------------
# Master instance definitions
# -----------------------------------------------------------------------------

resource "aws_instance" "graph-node" {
  ami = "${lookup(var.amis, var.aws_region)}"
  instance_type = "r5.2xlarge"
  key_name = "${var.aws_key_name}"
  availability_zone = "${var.aws_zone}"
  vpc_security_group_ids = ["${aws_security_group.neo4j.id}",
                            "${aws_security_group.ssh.id}",
                            "${aws_security_group.fragnet.id}"]
  subnet_id = "${var.aws_subnet_id}"
  source_dest_check = false

  associate_public_ip_address = true

  root_block_device {
    volume_size = "${var.data_volume_size}"
    volume_type = "gp2"
  }
}

#resource "aws_spot_instance_request" "graph-node" {
#  ami = "${lookup(var.amis, var.aws_region)}"
#  instance_type = "r5.2xlarge"
#  key_name = "${var.aws_key_name}"
#  vpc_security_group_ids = ["${aws_security_group.neo4j.id}",
#                            "${aws_security_group.ssh.id}"]
#  subnet_id = "${module.vpc.public_subnets[0]}"
#  associate_public_ip_address = true
#  source_dest_check = false
#  spot_price = "0.2"
#  spot_type = "one-time"
#  wait_for_fulfillment = true
#
#  root_block_device {
#    volume_type = "gp2"
#    volume_size = "100"
#  }
#}

#resource "aws_ebs_volume" "loader" {
#  availability_zone = "${var.aws_zone}"
#  type = "gp2"
#  size = 50
#}

#resource "aws_volume_attachment" "ebs_att" {
#  device_name = "/dev/sdh"
#  volume_id   = "${aws_ebs_volume.loader.id}"
#  instance_id = "${aws_instance.graph-node.0.id}"
#}
