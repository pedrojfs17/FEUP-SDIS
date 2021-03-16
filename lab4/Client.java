import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    public static void main(String[] args) throws IOException{
        if (args.length < 4 || args.length > 5) {
            System.out.println("Usage: java Client <host_name> <port_number> <oper> <opnd> * ");
            return;
        }

        String command = buildCommand(args);

        int port = Integer.parseInt(args[1]);

        Socket clientSocket = new Socket(args[0], port);

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        out.println(command);

        String received = in.readLine();

        System.out.println("Client: " + command + " : " + received);

        out.close();
        in.close();
        clientSocket.close();

        // while ((fromServer = in.readLine()) != null) {
        //     System.out.println("Server: " + fromServer);
        //     if (fromServer.equals("Bye."))
        //         break;
        
        //     fromUser = stdIn.readLine();
        //     if (fromUser != null) {
        //         System.out.println("Client: " + fromUser);
        //         out.println(fromUser);
        //     }
        // }
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