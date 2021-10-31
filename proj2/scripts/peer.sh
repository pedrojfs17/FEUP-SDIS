#! /usr/bin/bash

# Script for running a peer
# To be run in the root of the build tree
# No jar files used
# Assumes that Peer is the main class 
#  and that it belongs to the peer package
# Modify as appropriate, so that it can be run 
#  from the root of the compiled tree

# Check number input arguments

if [ "$#" -le 2 ]
then
	echo "Usage: $0 <access_point> <host_name> <host_port> [-b]"
	exit 1
fi

# Assign input arguments to nicely named variables

sap=$1
host_name=$2
host_port=$3
boot=$4

# Execute the program
# Should not need to change anything but the class and its package, unless you use any jar file

java backupservice.peer.Peer ${sap} ${host_name} ${host_port} ${boot}
