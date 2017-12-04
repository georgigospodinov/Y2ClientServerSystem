package server_side;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Represents a thread that waits for a command to turn off the {@link Server}.
 *
 * @author 150009974
 * @version 1.5
 */
class OffWaiter extends Thread {

    /**
     * The command that, when recognized, causes a system exit to be performed.
     */
    private static final String SHUTDOWN_CMD = "shutdown";

    /**
     * The command that, when recognized, stop the {@link Server} from listening
     * and once all current clients are served, exits nicely.
     */
    private static final String NICE_EXIT_CMD = "end";

    /**
     * The default value of the input string.
     * Assigned before the standard input is first read.
     */
    private static final String DEFAULT_INPUT = "default";

    /**
     * This flag tells whether the {@link Server} should be trying to stop,
     * i.e. stop accepting connections.
     * Initial value false - the server should not be trying to exit.
     * Value true - the server should be trying to exit.
     */
    private static boolean niceExit = false;

    /**
     * This flag tells whether a tread should recognize
     * the value of {@link OffWaiter#NICE_EXIT_CMD} as a command.
     * Value true - recognize it. Value false - ignore it.
     */
    private static boolean acceptNiceExit = true;

    /**
     * This flag tells the class should keep its instances alive.
     * It is set to false once the {@link Server} serves all of its clients
     * and no more user input is expected.
     */
    private static boolean keepAlive = true;

    /**
     * A getter for the {@link OffWaiter#niceExit} variable.
     * It is used in the class {@link Server} to recognize when it should no longer
     * offer its service.
     *
     * @return the value of the {@link OffWaiter#niceExit} flag.
     */
    static boolean isNiceExit() {
        return niceExit;
    }

    /**
     * Sets the value of the {@link OffWaiter#keepAlive} variable to false.
     * Changing the flag causes all instances of this class to complete
     * their {@link OffWaiter#run()} methods and thus, 'die'.
     */
    static void killAllInstances() {
        keepAlive = false;
    }

    /**
     * The thread recognizes user input from the standard input stream.
     * Input is converted to lower-case words and compared to the two possible commands
     * {@link OffWaiter#SHUTDOWN_CMD} and {@link OffWaiter#NICE_EXIT_CMD}.
     * Flags are changed when necessary and may exit the system.
     * Any other input is ignored.
     */
    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {

            String input = DEFAULT_INPUT;
            while (keepAlive) {

                if (reader.ready())
                    input = reader.readLine().toLowerCase(Locale.ENGLISH);

                if (acceptNiceExit && input.equals(NICE_EXIT_CMD)) {
                    niceExit = true;
                    System.out.println("Ending as soon as all active clients are served.");
                    System.out.println("Type \'shutdown\' to force stop.");
                    acceptNiceExit = false;
                }

                if (input.equals(SHUTDOWN_CMD)) {
                    System.out.println("Server terminated!");
                    System.exit(0);
                }

            }

        }
        catch (IOException e) {
            System.out.println("Could not read standard input!");
        }
    }

}
