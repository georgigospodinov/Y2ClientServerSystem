package server_side;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Represents a server that {@link client_side.Client}s can connect to and request files from.
 * It extends {@link Thread} because a new server thread is created whenever a client connects.
 * This way multiple clients can be served at the same time.
 *
 * @author 150009974
 * @version 2.0
 */
public class Server extends Thread implements util.NetworkAccessor {

    /**
     * The minimum port number on which a server can be started.
     */
    private static final int MIN_PORT = 1024;

    /**
     * The maximum port number on which a server can be started.
     */
    private static final int MAX_PORT = 65535;

    /**
     * The amount of milliseconds the server waits to accept a connection.
     * Thanks to this timeout, the condition of the while loop in {@link Server#main(String[])}
     * is periodically checked.
     */
    private static final int SERVER_TIMEOUT = 2000;

    /**
     * The maximum amount of errors a client can cause.
     * Prevents a server thread from getting stuck if its client is force-stopped.
     */
    private static final int MAX_ERROR_COUNT = 5;

    /**
     * The socket which this server thread uses to access the network.
     */
    private Socket connection;

    /**
     * The writer object that this server thread uses to send data over the network.
     */
    private PrintWriter netWriter = null;

    /**
     * The reader object that this server thread uses to read data over the network.
     */
    private BufferedReader netReader = null;

    /**
     * The address of the {@link client_side.Client} who is connected to this server thread.
     * It is used to identify message printed to the console when multiple clients are connected.
     */
    private String clientAddress;

    /**
     * This methods prints the commands that a user can issue (and their descriptions).
     * They are displayed only once when the server starts and
     * not whenever a new server thread is created.
     */
    private static void printCommands() {
        System.out.println("Commands:");
        System.out.println("end - quits the server as soon as all current clients are served");
        System.out.println("shutdown - forcefully quits the server");
        System.out.println();// Prints a new line for readability.
    }

    /**
     * A constructor for a new server thread.
     * The parameter should be the object returned by the {@link ServerSocket#accept()} method.
     * Instantiates the {@link Server#connection}, {@link Server#netWriter},
     * and {@link Server#netReader} objects.
     * @param connection the object returned by the {@link ServerSocket#accept()} method
     */
    private Server(Socket connection) {
        this.connection = connection;
        clientAddress = connection.getRemoteSocketAddress().toString();

        // Open a writer and then a reader to the network.
        try {
            netWriter = new PrintWriter(connection.getOutputStream(), true);
            netReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            System.out.println("Established connection to client " + clientAddress);
        }
        catch (IOException e) {
            System.out.println("Could not create network writer or reader!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeStreams() {

        // Close the network streams.
        if (netWriter != null) {
            netWriter.close();
        }

        if (netReader != null) {
            try {
                netReader.close();
            }
            catch (IOException e) {
                System.out.println("Could not close network reader to " + clientAddress);
            }
        }

        // Close the network connection socket.
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Closed connection socket to " + clientAddress);
            }
            catch (IOException e) {
                System.out.println("Could not close connection socket to " + clientAddress);
            }
        }

    }

    /**
     * Serves {@link client_side.Client} requests.
     * In a loop: receives a request from the {@link client_side.Client},
     * access the specified file, and returns its contents. Repeats until
     * the client sends a {@link util.NetworkAccessor#DISCONNECT_CMD} or too many failures occur.
     */
    @Override
    public void communicate() {

        // This case is reported by the constructor.
        if (netWriter == null || netReader == null)
            return;

        // This counts how many times reading from the client has failed.
        int errorCount = 0;

        System.out.println("Waiting for a request from " + clientAddress);
        String clientRequest;
        while (true) {

            if (errorCount == MAX_ERROR_COUNT) {
                System.out.println("Too many failures! Ending communication with " + clientAddress);
                return;
            }

            BufferedReader fileReader;
            try {
                // Wait for input over the network.
                clientRequest = netReader.readLine();

                if (clientRequest.equals(DISCONNECT_CMD)) {
                    System.out.println(clientAddress + " disconnected!");
                    break;
                }
                System.out.println(clientAddress + " requested file: " + clientRequest);

                // Locate and open the file that the client wants.
                clientRequest = FileAccess.getFullPath(clientRequest);
                if (clientRequest != null) {
                    fileReader = new BufferedReader(new FileReader(clientRequest));
                }
                else throw new FileNotFoundException();

            }
            catch (FileNotFoundException e) {
                System.out.println(NO_SUCH_FILE);
                netWriter.println(NO_SUCH_FILE);
                continue;
            }
            catch (NullPointerException e) {
                // This exception is thrown by the netReader object when the client is forcefully shutdown.
                System.out.println("Lost connection to client " + clientAddress);
                break;
            }
            catch (IOException e) {
                System.out.println("Could not read client " + clientAddress + "\'s request from network!");
                errorCount++;
                continue;
            }

            // Read and send one line at a time.
            try {
                String serverResponse = fileReader.readLine();
                if (serverResponse == null) {
                    System.out.println("No content to send.");
                    netWriter.println(NO_CONTENT);
                }
                else {
                    System.out.println("Sending contents...");
                    while (serverResponse != null) {
                        netWriter.println(serverResponse);
                        serverResponse = fileReader.readLine();
                    }
                    System.out.println("Sending complete.");
                }
            }
            catch (IOException e) {
                System.out.println(FILE_READ_FAILED);
                netWriter.println(FILE_READ_FAILED);
            }

            // Close the file reader
            try {
                fileReader.close();
            }
            catch (IOException e) {
                System.out.println("Could not close the file reader!");
            }

            System.out.println("Waiting for a new request from " + clientAddress);
        }

    }

    /**
     * The server thread's run method simply calls the {@link Server#communicate()} and
     * {@link Server#closeStreams()} methods.
     */
    @Override
    public void run() {

        communicate();
        closeStreams();
        // Print a new line for readability.
        System.out.println();

    }

    /**
     * The main method through which the server is started.
     * Loads all the accessible directories,
     * starts an {@link OffWaiter} thread,
     * starts the server on a specified port (or the default one),
     * sets a timeout of two seconds (this allows for the main loop to check its condition)
     * listens for incoming connections and create new threads.
     *
     * @param args the directories which the server can access.
     */
    public static void main(String[] args) {

        FileAccess.loadDirectories(args);

        int portNumber = WELL_KNOWN_PORT;
        System.out.print("Which port would you like to start the server on?[" + WELL_KNOWN_PORT + "]");
        try {
            String input = new BufferedReader(new InputStreamReader(System.in)).readLine();
            if (input.equals("")) {
                System.out.println("Starting server on default port...");
            }
            else {
                portNumber = Integer.parseInt(input);
                if (portNumber < MIN_PORT || portNumber > MAX_PORT) {
                    throw new NumberFormatException();
                }
            }
        }
        catch (NumberFormatException e) {
            System.out.println("Could not parse the port number, using default port instead.");
            portNumber = WELL_KNOWN_PORT;
        }
        catch (IOException e) {
            System.out.println("Could not read user input!");
        }


        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server started!");
        }
        catch (IOException e) {
            System.out.println("Could not start server!");
            return;
        }

        printCommands();

        // Background thread that recognizes ending commands.
        new OffWaiter().start();

        // Set server socket timeout.
        try {
            serverSocket.setSoTimeout(SERVER_TIMEOUT);
        }
        catch (SocketException e) {
            System.out.println("Could not set server socket time out.");
        }

        Server serverThread = null;
        while (!OffWaiter.isNiceExit()) {
            try {
                serverThread = new Server(serverSocket.accept());
                serverThread.start();
            }
            catch (SocketTimeoutException ignored) {
                // This is thrown when the accept times out. It allows a nice exit from the loop.
            }
            catch (IOException e) {
                System.out.println("Could not accept connection!");
            }
        }

        // Wait for the server threads to finish.
        if (serverThread != null) {
            //noinspection StatementWithEmptyBody
            while (serverThread.isAlive()) ;
        }

        OffWaiter.killAllInstances();

        // Close server socket.
        try {
            serverSocket.close();
        }
        catch (IOException e) {
            System.out.println("Could not close server socket!");
        }

    }
}

