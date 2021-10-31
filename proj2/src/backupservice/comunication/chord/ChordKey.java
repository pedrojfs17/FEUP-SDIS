package backupservice.comunication.chord;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChordKey implements Serializable {
    public static final int M_BIT = 8;
    public static final int MAX = (int) Math.pow(2, M_BIT);
    private int key;
    private final InetSocketAddress address;

    public ChordKey(int key, InetSocketAddress address) {
        this.key = (key & 0x7fffffff % MAX);
        this.address = address;
    }

    public ChordKey(int key) {
        this.key = key;
        this.address = null;
    }

    public static ChordKey getKeyFromAddress(InetSocketAddress address) {
        try {
            return new ChordKey(hashAddress(address).hashCode(), address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String hashAddress(InetSocketAddress address) throws NoSuchAlgorithmException {
        String addressIdentifier = address.getHostString() + address.getPort();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new String(digest.digest(addressIdentifier.getBytes(StandardCharsets.UTF_8)));
    }

    public static ChordKey fromString(String key) {
        String address = key.substring(0, key.indexOf(":"));
        int port = Integer.parseInt(key.substring(key.indexOf(":") + 1, key.lastIndexOf(":")));
        int keyId = Integer.parseInt(key.substring(key.lastIndexOf(":") + 1));
        return new ChordKey(keyId, new InetSocketAddress(address, port));
    }

    public boolean between(ChordKey lowerBound, ChordKey upperBound, boolean exclusive) {
        if (!exclusive)
            return (lowerBound.key < upperBound.key) ? this.key >= lowerBound.key && this.key <= upperBound.key : this.key >= lowerBound.key || this.key <= upperBound.key;
        return (lowerBound.key < upperBound.key) ? this.key > lowerBound.key && this.key <= upperBound.key : this.key > lowerBound.key || this.key <= upperBound.key;
    }

    public int shiftKey(int i) {
        return (int) ((this.key + Math.pow(2, i - 1)) % MAX);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChordKey chordKey = (ChordKey) o;
        return key == chordKey.key;
    }

    @Override
    public int hashCode() {
        return this.key;
    }

    public int getKey() {
        return key;
    }

    public InetSocketAddress getAddress() { return address; }

    public String getString() { return address.getHostString() + ":" + address.getPort() + ":" + key; }

    public void setId(int id) {
        this.key = id;
    }

    public void incrementKey(){
        this.key+=1;
        if(this.key>MAX)
            this.key=0;
    }

    @Override
    public String toString() {
        return "ChordKey{" +
                "key=" + key +
                ", address=" + address +
                '}';
    }
}
