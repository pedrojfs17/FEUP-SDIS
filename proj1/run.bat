start cmd /k java -jar McastSnooper.jar 238.0.0.1:8001 238.0.0.2:8002 238.0.0.3:8003

sh scripts/compile.sh

cd build
start rmiregistry

start cmd /k sh ../scripts/peer.sh 2.0 1 peer1 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003
start cmd /k sh ../scripts/peer.sh 2.0 2 peer2 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003
start cmd /k sh ../scripts/peer.sh 2.0 3 peer3 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003
start cmd /k sh ../scripts/peer.sh 2.0 4 peer4 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003
start cmd /k sh ../scripts/peer.sh 2.0 5 peer5 238.0.0.1 8001 238.0.0.2 8002 238.0.0.3 8003