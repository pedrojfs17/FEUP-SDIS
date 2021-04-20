import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

public class SSLClient {
    public static void main(String[] args) throws IOException{
        if (args.length < 4) {
            System.out.println("Usage: java SSLClient <host> <port> <oper> <opnd>* <cypher-suite>* ");
            return;
        }

        // Truststore
        System.setProperty("javax.net.ssl.trustStore", "truststore");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        // Keystore
        System.setProperty("javax.net.ssl.keyStore", "clientKeyStore");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        String command = buildCommand(args);

        int port = Integer.parseInt(args[1]);

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket clientSocket = (SSLSocket) factory.createSocket(args[0], port);

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        out.println(command);

        String received = in.readLine();

        System.out.println("SSLClient: " + command + " : " + received);

        out.close();
        in.close();
        clientSocket.close();
    }

    private static String buildCommand(String[] args) {
        String[] ops = new String[args.length - 2];
        System.arraycopy(args, 2, ops, 0, args.length - 2);

        if (ops.length == 2)
            return String.join(" ", ops[0], ops[1]);
        else if (ops.length == 3)
            return String.join(" ", ops[0], ops[1], ops[2]);

        return "";
    }
}