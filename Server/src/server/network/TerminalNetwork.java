package server.network;

import model.Event;
import network.data.Message;
import network.JsonSocket;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is responsible for create and handling the communication between users and the <code>Terminal</code>
 * <p>
 * A new object from this class will be created when a <code>Terminal</code> wants to start.
 * <p>
 * All of the required efforts for connecting a client to that server is done by the parent class called {@link server.network.NetServer}
 * so in this class we just need to handle the events that are occur after user successfully connects with
 *
 * @see server.network.TerminalNetwork.TerminalNetworkThread
 * @see server.network.NetServer
 */
public class TerminalNetwork extends NetServer {

    public static final int MAX_RECEIVE_EXCEPTIONS = 20;
    public static final String REPORT_NAME = "report";

    /**
     * used to identify the users to connect to Terminal.
     */
    private String token;

    /**
     * This interface is used for transfer <code>Commands</code> and <code>Events</code>.
     * This interface is implemented in {@link server.core.GameHandler}.
     * <code>TerminalInterface</code> has two methods called <code>putEvent</code> and <code>runCommand</code>
     * that will be used for sending essential data to {@link server.core.GameHandler}.
     */
    public interface TerminalInterface {
        void putEvent(Event event);
        Message runCommand(Message command);
    }

    /**
     * An instance of {@link server.network.TerminalNetwork} used to
     */
    private TerminalInterface handler;

    @Override
    protected void accept(JsonSocket client) {
        new TerminalNetworkThread(client).start();
    }

    public TerminalNetwork(String token) {
        this.token = token;
    }

    public void setHandler(TerminalInterface handler) {
        this.handler = handler;
    }

    /**
     * This thread is used by {@link server.network.TerminalNetwork}
     * to handle multiple clients that want to start a terminal.
     * <p>
     * {@link server.network.TerminalNetwork.TerminalNetworkThread} uses a {@link network.JsonSocket}
     * to get the inputs of a client. The first input is a <code>Token</code>.
     * If user enter a correct <code>Token</code> then terminal actually starts.
     * <p>
     * Then it converts every input to a {@link network.data.Message} class
     * and if the input was a valid one it checks whether this input is command or event.
     * Then it creates the appropriate object and by means of {@link server.network.TerminalNetwork.TerminalInterface}
     * sends it to {@link model.Event}.
     *
     * @see server.network.NetServer
     * @see network.JsonSocket
     */
    class TerminalNetworkThread extends Thread  {

        /**
         * Use for get the user input.
         */
        private JsonSocket client;

        /**
         * For making an infinite loop. there's no need to make it false because when the terminal actually
         * terminated, the thread will be stop.
         */
        private boolean userHasCommand = true;

        /**
         * Constructor.
         * @param client use to get the input from user
         */
        public TerminalNetworkThread (JsonSocket client) {
            this.client = client;
        }

        /**
         * Check tokens and analyze the inputs of the user.
         *
         * @see #TerminalNetworkThread(JsonSocket)
         * @see #checkInput()
         */
        @Override
        public void run() {
            try {
                Message msg = client.get(Message.class);

                if (!msg.name.equals("token") || msg.args == null || msg.args.length < 1 || !msg.args[0].equals(token)) {
                    client.close();
                    return;
                }
                Object [] args = new Object[1];
                args[0] = new ArrayList<String>();
                client.send(new Message("init", args));

                int exceptions = 0;

                // now the user is a valid one
                while (userHasCommand) {
                    try {
                        checkInput();
                    } catch (Exception e) {
                        exceptions++;
                        if (exceptions > MAX_RECEIVE_EXCEPTIONS)
                            return;
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        /**
         * If input was not a command or an event then this method throws an IOException
         * @throws IOException if the input is invalid
         */
        public void checkInput() throws IOException {
            Message msg = client.get(Message.class);
            switch (msg.name) {
                case "command":
                    Message command  = new Message((String)msg.args[0], ((ArrayList<String>)msg.args[1]).toArray());
                    Message report = handler.runCommand(command);
                    client.send(report);
                    break;
                case Event.EVENT:
                    handler.putEvent((Event) msg.args[0]);
                    break;
                default:
                    client.send(new Message(REPORT_NAME, new Object[]{
                            new Object[] {"This message is not defined."}
                    }));
                    //client.send("Your input is invalid!\nPlease don't hack me :)");
                    break;
            }
        }

    }

}