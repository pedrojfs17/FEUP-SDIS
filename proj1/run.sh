#! /bin/bash

gnome-terminal --title "McastSnooper" -- /bin/bash -c "java -jar McastSnooper.jar 238.0.0.1:8001 238.0.0.2:8002 238.0.0.3:8003" &

cd src

sh ../scripts/compile.sh

cd build

gnome-terminal --title "Peer1" -- /bin/bash -c "sh ../../scripts/peer.sh 2.0 1 peer1 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003" &
gnome-terminal --title "Peer2" -- /bin/bash -c "sh ../../scripts/peer.sh 2.0 2 peer2 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003" &
gnome-terminal --title "Peer3" -- /bin/bash -c "sh ../../scripts/peer.sh 2.0 3 peer3 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003" &
gnome-terminal --title "Peer4" -- /bin/bash -c "sh ../../scripts/peer.sh 2.0 4 peer4 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003" &
gnome-terminal --title "Peer5" -- /bin/bash -c "sh ../../scripts/peer.sh 2.0 5 peer5 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003"
