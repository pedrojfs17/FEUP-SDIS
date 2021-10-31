# SDIS Project

SDIS Project for group T4G25.

Group members:

1. António Bezerra (up201806854@fe.up.pt)
2. Gonçalo Alves (up201806451@fe.up.pt)
3. Inês Silva (up201806385@fe.up.pt)
4. Pedro Seixas (up201806227@fe.up.pt)

## Compilation

In order to compile the source code, please run the following command from the `src` folder:
```
sh ../scripts/compile.sh
```

## Execution

### Peer

In order to start a single peer please run the following command from the `src/build` folder, created after compilation:
```
sh ../../scripts/peer.sh <access_point> <host_name> <host_port> [-b]
```
Where:
- `<access_point>` is service access point of the peer used for RMI
- `<host_name>` and `<host_port>` is the address and port to join the CHORD ring
- `[-b]` is a flag to start a peer as a boot peer or not. If present, this peer will create a new CHORD ring. If not, this peer will try to join the CHORD ring where the peer with `<host_name>:<host_port>` is part of.

To simplify the execution, there is a script which can be run from the same folder (`src/build`) that initializes a CHORD ring with 4 peers in it. To run this script please run the following command:
```
sh ../../scripts/peers.sh
```

### Client

In order to start a client and send a request to a peer please run the following command from the `src/build` folder:
```
sh ../../scripts/test.sh <access_point> <operation> [<operand_1> [<operand_2]]
```
Where:
- `<access_point>` is service access point
- `<operation>` is the desired protocol: BACKUP|RESTORE|DELETE|RECLAIM|STATE
- `<operandX>` are the arguments needed for that protocol