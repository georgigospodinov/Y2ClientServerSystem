package client_side;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Represents a client that can connect to a {@link server_side.Server} and request files.
 *
 * @author 150009974
 * @version 2.0
 */
public class Client implements util.NetworkAccessor {

    /**
     * The get command which should be recognized from the standard input stream
     * and should result in a request sent to a {@link server_side.Server}.
     */
    private static final String GET_CMD = "get ";

    /**
     * The connect command which should be recognized from the standard input stream
     * and should result in disconnecting from the current {@link server_side.Server} (if any)
     * and connecting to a new one.
     */
    private static final String CONNECT_CMD = "connect";

    /**
     * The end command which should be recognized from the standard input stream
     * and should result in shutting down the process.
     */
    private static final String END_CMD = "end";

    /**
     * The request that gets send to the {@link server_side.Server}.
     */
    private String clientRequest;

    /**
     * The network socket this client uses to communicate.
     */
    private Socket netSocket;

    /**
     * The writer object this client uses to send data over the network.
     */
    private PrintWriter netWriter;

    /**
     * The reader object this client uses to receive data over the network.
     */
    private BufferedReader netReader;

    /**
     * This methods prints the commands that a user can issue (and their descriptions).
     */
    private static void printCommands() {
        System.out.println("Commands:");
        System.out.println("end - quits the program");
        System.out.println("get <file_name> - retrieves a file with the given name from the server");
        System.out.println("connect <ip_address>:<port> - connects to a service at the given socket");
        System.out.println("connect - connects to the default service localhost:"+WELL_KNOWN_PORT);
        System.out.println();// Prints a new line for readability.
    }

    /**
     * Connects to a {@link server_side.Server}socket given as a string.
     * Separates the host part from the port part and calls {@link Client#openStreams(String, int)}.
     *
     * @param ipSocket the socket of a sever as a string
     */
    private void openStreams(String ipSocket) {
        if (ipSocket == null) {
            System.out.println("Invalid ip socket!");
            return;
        }

        if (ipSocket.equals("")) {
            System.out.println("Trying default connection to localhost:"+WELL_KNOWN_PORT);
            openStreams("localhost", WELL_KNOWN_PORT);
            return;
        }

        int positionOfColon = ipSocket.indexOf(':');
        /*
         * The 0-th position is the space after the CONNECT_CMD
         * and the host should be at least one character long.
         * Thus the colon should be at least at position two.
         */
        if (positionOfColon < 2) {
            System.out.println("Invalid ip socket!");
            return;
        }

        // The 0-th position is the space between "connect" and the socket.
        String hostPart = ipSocket.substring(1, positionOfColon);
        String portPart = ipSocket.substring(positionOfColon + 1);
        int port;
        try {
            port = Integer.parseInt(portPart);
        }
        catch (NumberFormatException e) {
            System.out.println("Invalid port!");
            return;
        }

        openStreams(hostPart, port);
    }

    /**
     * Connects to a given host on a given port.
     * Instantiates the {@link Client#netSocket}, {@link Client#netWriter},
     * and {@link Client#netReader} objects.
     *
     * @param host the host to which the client should connect
     * @param port the port on which the client should connect
     */
    private void openStreams(String host, int port) {

        try {
            netSocket = new Socket(host, port);
        }
        catch (UnknownHostException e) {
            System.out.println("Could not find the host to connect to!");
            return;
        }
        catch (IOException e) {
            System.out.println("Could not instantiate client socket!");
            return;
        }

        // Open a writer and then a reader to the network.
        try {
            netWriter = new PrintWriter(netSocket.getOutputStream(), true);
            netReader = new BufferedReader(new InputStreamReader(netSocket.getInputStream()));
        }
        catch (IOException e) {
            System.out.println("Could not instantiate network writer or writer!");
            return;
        }

        System.out.println("Connection established.");

    }

    /**
     * Sets the value of the {@link Client#clientRequest} variable.
     * The {@link Client#communicate()} method then sends this value over the network.
     * That value should be the name of a file.
     *
     * @param clientRequest the name of the file which will be requested
     */
    private void setClientRequest(String clientRequest) {
        this.clientRequest = clientRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeStreams() {

        // Close the network streams.
        if (netWriter != null) {
            netWriter.println(DISCONNECT_CMD);// Send a disconnect command.
            netWriter.close();
        }

        if (netReader != null) {
            try {
                netReader.close();
            }
            catch (IOException e) {
                System.out.println("Could not close network reader!");
            }
        }

        // Close the client socket.
        if (netSocket != null) {
            try {
                netSocket.close();
            }
            catch (IOException e) {
                System.out.println("Could not close client socket!");
            }
        }

    }

    /**
     * Sends the current value of the {@link Client#clientRequest} variable over the network
     * and then prints the {@link server_side.Server}'s response.
     * That response may be an error description which will be indicated.
     */
    @Override
    public void communicate() {

        if (netWriter == null || netReader == null) {
            System.out.println("Cannot process request, try re-connecting!");
            return;
        }

        // Send client input.
        System.out.println("File requested:" + clientRequest);
        netWriter.println(clientRequest);

        try {
            System.out.println("Waiting for response...");
            String networkResponse = netReader.readLine();

            System.out.println("Received response:");
            switch (networkResponse) {

                // In the case of an error, report it.
                case NO_SUCH_FILE:
                case FILE_READ_FAILED:
                case NO_CONTENT:
                    System.out.println("Error: " + networkResponse);
                    break;

                // But by default, there is a valid response.
                default:
                    System.out.println(networkResponse);
                    while (netReader.ready()) {
                        networkResponse = netReader.readLine();
                        System.out.println(networkResponse);
                    }
                    break;
            }

        }
        catch (NullPointerException | IOException e) {
            System.out.println("Could not receive server response!");
        }

    }

    /**
     * The method that is used to run the class.
     * Instantiates a client and reads user input.
     *
     * @param args command line arguments are ignored
     */
    public static void main(String[] args) {

        if (args.length != 0) {
            System.out.println("Command-line arguments are ignored!");
        }

        printCommands();

        Client client = new Client();
        System.out.println("Connecting to default service...");
        client.openStreams("localhost", WELL_KNOWN_PORT);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        while (true) {
            System.out.print(">");
            try {
                userInput = reader.readLine().toLowerCase();
            }
            catch (IOException e) {
                System.out.println("Could not read user input.");
                break;
            }
            if (userInput.startsWith(CONNECT_CMD)) {
                client.closeStreams();// Close any open streams.
                client.openStreams(userInput.substring(CONNECT_CMD.length()));// Open new ones.
            }
            if (userInput.startsWith(GET_CMD)) {
                client.setClientRequest(userInput.substring(GET_CMD.length()));
                client.communicate();
            }
            if (userInput.startsWith(END_CMD)) {
                break;
            }
        }

        client.closeStreams();

    }

}
