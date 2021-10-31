package backupservice.comunication.chord;

import java.net.InetSocketAddress;

public interface Chord {

    //Get node successor
    ChordKey getSucc();

    //Get node predecessor
    ChordKey getPre();

    // Fetches Chunk/File
    ChordKey lookup(ChordKey key);

    //Replaces a Peer in the FT
    void replacePeer(ChordKey key, ChordKey newKey);

    // Get ith finger of table
    // i = 1...m
    ChordKey getFinger(int i);

    //Set ith finger in table
    void setFinger(int i, ChordKey key);

    //Set Predecessor
    void setPre(ChordKey key);

    //Create chord ring
    void create();

    //Leave chord ring
    void leave();

    //Join chord ring
    //address is the address of the boot peer
    void join(InetSocketAddress address);

    //Notify node that the given node is predecessor
    void notify(ChordKey node, ChordKey potentialPredecessor);

    //Set predecessor as address
    void notified(ChordKey address);

    //Find key successor
    ChordKey findSucc(ChordKey key);

    ChordKey getClosestPre(ChordKey key);

}
