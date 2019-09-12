# Snapshot and restore on AWS

Brief instructions for creating a snapshot of the fragnet server and restoring from snapshot.

## Creating snapshot

1. Shut down server.
2. Create snapshot of root volume.
3. Create snapshot of graph db volume.
4. Create image from snapshot of root volume.

## Restoring

1. Create volume from graph db snapshot.
2. Create instance from image (image is in "My AMIs" section), instance type r5.xlarge.
   - Specify Squonk network.
   - Specify to create public IP address.
   - Specify the 3 security groups.
3. Once instance is running:
   - Mount new graph db volume at `/dev/sdg`.
   - Assign public IP address to instance.
   - (maybe) restart instance.