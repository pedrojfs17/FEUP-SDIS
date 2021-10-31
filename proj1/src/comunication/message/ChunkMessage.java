package comunication.message;

import peer.Peer;
import utils.Utils;

import java.io.IOException;

public class ChunkMessage extends Message {
    public ChunkMessage(Peer peer, String[] header, byte[] body) {
        super(peer);

        this.version = Double.parseDouble(header[0]);
        this.senderId = Integer.parseInt(header[2]);
        this.fileId = header[3];
        this.chunkNo = Integer.parseInt(header[4]);

        this.body = body;
    }

    @Override
    public void handleMessage() {
        if (peer.isFileOwner(this.fileId)) {
            try {
                peer.composeFile(this.fileId, this.chunkNo, this.body, version);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            peer.updateRestoreQueue(this.fileId + "_" + this.chunkNo);
    }

    public static byte[] buildMessage(String version, int peerId, String fileId, int chunkNo, byte[] body) {
        byte[] header = (version + " CHUNK "
                + peerId + " "
                + fileId + " "
                + chunkNo + " " + Utils.CRLF + Utils.CRLF).getBytes();

        byte[] msg = new byte[header.length + body.length];

        System.arraycopy(header, 0, msg, 0, header.length);
        System.arraycopy(body, 0, msg, header.length, body.length);

        return msg;
    }
}
