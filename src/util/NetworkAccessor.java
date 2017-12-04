package util;

/**
 * This interface contains constants that should be recognized by communicating processes.
 * It also has two methods that should describe how a process accesses the network.
 *
 * @author 150009974
 * @version 2.0
 */
public interface NetworkAccessor {

    /**
     * The message displayed and passed when a non-existing file is requested.
     */
    String NO_SUCH_FILE = "\"No such file!\"";

    /**
     * The message displayed and passed when an IO exception occurs during file read.
     */
    String FILE_READ_FAILED = "\"Failed to read file!\"";

    /**
     * The message displayed and passed when the requested file is empty.
     */
    String NO_CONTENT = "\"File lacks content!\"";

    /**
     * The disconnect command that is passed to end a connection
     * between a {@link client_side.Client} and a {@link server_side.Server}.
     */
    String DISCONNECT_CMD = "disconnect";

    /**
     * The default well-known port on which the {@link server_side.Server} runs
     * and to which the {@link client_side.Client} tries to connect.
     */
    int WELL_KNOWN_PORT = 12345;

    /**
     * Closes all open streams.
     * Does null checks to avoid NullPointerException-s.
     */
    void closeStreams();

    /**
     * Exchanges data with another node.
     * This default documentation is overwritten in the classes implementing this interface.
     */
    void communicate();
}
