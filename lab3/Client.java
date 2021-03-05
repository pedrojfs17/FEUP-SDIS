import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}

    public static void main(String[] args) {

        if (args.length < 4 || args.length > 5) {
            System.out.println("Usage: java Client <host_name> <remote_object_name> <oper> <opnd>*");
            return;
        }

        String command = buildCommand(args);

        try {
            Registry registry = LocateRegistry.getRegistry(args[0]);
            RMI stub = (RMI) registry.lookup(args[1]);
            String response = stub.request(command);

            System.out.println(command + " :: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

    }

    public static String buildCommand(String[] args) {
        String[] ops = new String[args.length - 2];
        System.arraycopy(args, 2, ops, 0, args.length - 2);

        if (ops.length == 2)
            return String.join(" ", ops[0], ops[1]);
        else if (ops.length == 3)
            return String.join(" ", ops[0], ops[1], ops[2]);

        return "";
    }
}