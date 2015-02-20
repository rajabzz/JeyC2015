package client;

import network.JsonSocket;
import network.data.Message;
import network.data.ReceivedMessage;
import util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

/**
 * {@link Network} is responsible for connecting to server, and
 * sending/receiving messages to/from server.
 */
public class Network {

    /**
     * Logging tag.
     */
    private static final String TAG = "Network";

    /**
     * Maximum number of exceptions could occur during connection. After that
     * client will be closed.
     */
    public static final int MAX_NUM_EXCEPTIONS = 50;

    /**
     * Handles incoming messages.
     */
    private Consumer<ReceivedMessage> messageHandler;

    /**
     * Send queue.
     */
    private LinkedBlockingDeque<Message> messagesToSend;

    /**
     * Connection details.
     */
    private int port;
    private String host;
    private String token;

    /**
     * Socket of the client.
     */
    private JsonSocket socket;

    /**
     * Connection flag.
     */
    private boolean isConnected;

    /**
     * Executor used to receive messages.
     */
    private ExecutorService executor;

    /**
     * Termination flag.
     */
    private boolean terminateFlag;

    /**
     * Number of exceptions occurred during communication.
     */
    private int numOfExceptions;


    /**
     * Constructor.
     *
     * @param messageHandler    handles incoming messages
     */
    public Network(Consumer<ReceivedMessage> messageHandler) {
        this.messageHandler = messageHandler;
        messagesToSend = new LinkedBlockingDeque<>();
        executor = Executors.newCachedThreadPool();
        startSending();
    }

    /**
     * Setter for connection details.
     *
     * @param host     server's host (ip)
     * @param port     server's port
     * @param token    client's token
     */
    public void setConnectionData(String host, int port, String token) {
        this.host = host;
        this.port = port;
        this.token = token;
    }

    /**
     * Tries to connect and send token to the server.
     */
    public void connect() {
        isConnected = false;
        JsonSocket client;
        ReceivedMessage init;
        try {
            client = new JsonSocket(host, port);
            client.send(new Message("token", new Object[]{token}));
            init = client.get(ReceivedMessage.class);
            if (!init.name.equals("init")) {
                client.close();
                throw new Exception("First message of the server was not init message.");
            }
        } catch (IOException e) {
            Log.i(TAG, "Error while connection to server.", e);
            handleIOE(e);
            return;
        } catch (Exception e) {
            Log.i(TAG, "Error while connection to server.", e);
            return;
        }
        isConnected = true;
        this.socket = client;
        messageHandler.accept(init);
        startReceiving();
    }

    /**
     * Starts listening for the server messages.
     */
    private void startReceiving() {
        executor.submit(() -> {
            while (!terminateFlag)
                doReceive();
            executor.shutdownNow();
            executor = null;
        });
    }

    /**
     * Listens for a single message of the server.
     */
    private void doReceive() {
        try {
            messageHandler.accept(socket.get(ReceivedMessage.class));
        } catch (IOException e) {
            Log.i(TAG, "Error receiving the server's message.", e);
            handleIOE(e);
        } catch (Exception e) {
            Log.i(TAG, "Error receiving the server's message.", e);
        }
    }

    /**
     * Starts sending messages to server.
     */
    private void startSending() {
        executor.submit(() -> {
            while (!terminateFlag) {
                try {
                    Message msg = messagesToSend.take();
                    socket.send(msg);
                } catch (InterruptedException ignored) {
                } catch (IOException e) {
                    Log.i(TAG, "Error while sending client's message.", e);
                    handleIOE(e);
                }
            }
        });
    }

    /**
     * Sends a message to the server.
     *
     * @param msg    message to send
     */
    public void send(Message msg) {
        messagesToSend.add(msg);
    }

    /**
     * Terminates operations of the network.
     */
    public void terminate() {
        terminateFlag = true;
        try {
            socket.close();
        } catch (IOException e) {
            Log.i(TAG, "Error closing the client.", e);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isTerminated() {
        return terminateFlag;
    }

    private void handleIOE(IOException e) {
        numOfExceptions++;
        if (numOfExceptions > MAX_NUM_EXCEPTIONS)
            terminate();
    }

}
