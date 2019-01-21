# -----------------------------------------------------------------------------
# Security groups
# -----------------------------------------------------------------------------

resource "aws_security_group" "neo4j" {
  name = "Neo4j Security Group"
  vpc_id = "${var.aws_vpc_id}"

  ingress {
    from_port = 7474
    to_port = 7474
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port = 7687
    to_port = 7687
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "fragnet" {
  name = "FragNet Security Group"
  vpc_id = "${var.aws_vpc_id}"

  ingress {
    from_port = 3080
    to_port = 3080
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ssh" {
  name = "SSH Security Group"
  vpc_id = "${var.aws_vpc_id}"

  # SSH in
  ingress {
    protocol = "tcp"
    from_port = 22
    to_port = 22
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Open to receive anything from anyone (internally)...
  ingress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    self = true
  }

  # Any outbound traffic
  egress {
    protocol = "-1"
    from_port = 0
    to_port = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}
