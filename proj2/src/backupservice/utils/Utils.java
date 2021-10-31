package backupservice.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static String CRLF = "\r\n";

    public static byte[] hashKey(String fileName) throws IOException, NoSuchAlgorithmException {
        BasicFileAttributes attributes = Files.readAttributes(FileSystems.getDefault().getPath(System.getProperty("user.dir") + "/" + fileName), BasicFileAttributes.class);

        String fileIdentifier = fileName + attributes.size() + attributes.creationTime().toString() + attributes.lastModifiedTime().toString();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        return digest.digest(fileIdentifier.getBytes(StandardCharsets.UTF_8));
    }

    public static String bytesToHexString(byte[] bytes) {
        byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static int findByte(byte[] bytes, String bytesToFind) {
        byte[] toFind = bytesToFind.getBytes();

        for (int i = 0; i < bytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < toFind.length; j++) {
                if (bytes[i + j] != toFind[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }

        return -1;
    }

    public static String getFileName(String path) {
        Path p = Paths.get(path);
        return p.getFileName().toString();
    }
}
