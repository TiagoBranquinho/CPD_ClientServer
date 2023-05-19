// ---------------------------------------------------------------------------------------------------

import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

// ---------------------------------------------------------------------------------------------------

public class Client {
    private static final int PORT = 5000;
    private static final int TIMEOUT = 10000;

    private SocketChannel channel;
    private ByteBuffer buffer;
    private Scanner scanner;

// ---------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.launch();
    }

    public Client() {
        this.buffer = ByteBuffer.allocate(1024);
        this.scanner = new Scanner(System.in);
    }

// ---------------------------------------------------------------------------------------------------

    private void launch() {
        try {
            this.channel = SocketChannel.open();
            this.channel.connect(new InetSocketAddress(PORT));

            while (true)
                if (!this.connect())
                        break;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

// ---------------------------------------------------------------------------------------------------

    public SocketChannel getChannel() {
        return this.channel;
    }

// ---------------------------------------------------------------------------------------------------

    private String[] readInput() {
        String input = this.scanner.nextLine();
        return input.split("\\s+");
    }

    public String readMessage() throws IOException {
        this.buffer.clear();
        int bytes_read = this.channel.read(this.buffer);
        this.buffer.flip();

        if (bytes_read >= 0)
            return new String(this.buffer.array(), 0, bytes_read).trim();

        return "error";
    }

    public void writeMessage(String message) throws IOException {
        this.buffer.clear();
        this.buffer.put(message.getBytes());
        this.buffer.flip();

        this.channel.write(this.buffer);
    }

// ---------------------------------------------------------------------------------------------------

    private boolean connect() throws IOException {
        System.out.println("'R' to REGISTER, 'L' to LOGIN, or '^C' to QUIT (at anytime)");

        String message = "";
        String input[] = this.readInput();
        String s = input[0].toLowerCase();

        switch (s) {
            case "r":
                this.writeMessage("[CONNECT] " + s);
                message = this.readMessage();
                if (message.equals("Message received"))
                    if (!this.register())
                        return false;
                break;
            case "l":
                this.writeMessage("[CONNECT] " + s);
                message = this.readMessage();
                if (message.equals("Message received"))
                    if (!this.login())
                        return false;
                break;
            case "q":
                this.writeMessage("[CONNECT] " + s);
                message = this.readMessage();
                if (message.equals("Message received")) {
                    System.out.println("Goodbye!");
                    return false;
                }
                break;
            default:
                System.out.println("Invalid input");
                return false;
        }

        return true;
    }

    private boolean register() throws IOException {
        System.out.println("Register in this format: username password");
        String input[] = this.readInput();

        if (input.length != 2){
            System.out.println("Please attend to the requested format");
            System.out.println("Neither the username nor the password should contain spaces");
            return false;
        }

        this.writeMessage("[REGISTER] " + input[0] + " " + input[1]);
        String message = this.readMessage();
        if (!message.equals("Message received")) {
            System.out.println(message);
            return false;
        }

        System.out.println("Registration complete!");
        return true;
    }

    private boolean login() throws IOException {
        System.out.println("Login in this format: username password");
        String input[] = this.readInput();

        if (input.length != 2) {
            System.out.println("Please attend to the requested format");
            System.out.println("Neither the username nor the password should contain spaces");
            return false;
        }

        this.writeMessage("[LOGIN] " + input[0] + " " + input[1]);
        String message = this.readMessage();
        if (!message.equals("Message received")) {
            System.out.println(message);
            return false;
        }

        System.out.println("Authentication complete");
        return this.queue();
    }

    private boolean queue() throws IOException {
        this.writeMessage("[QUEUE] ");

        System.out.println("You were added to the queue");
        return this.playGame();
    }

    private boolean playGame() throws IOException {
        while (true) {
            String message = this.readMessage();
            System.out.println(message);

            String[] split_message = message.split("]");
            String identifier = split_message[0];

            switch (identifier) {
                case "[INFO":
                    this.writeMessage("Message received");
                    break;
                case "[PLAY":
                    this.play();
                    break;
                case "[EXIT":
                    int global_score = Integer.parseInt(split_message[1].trim());
                    System.out.println("Your updated score is " + global_score);
                    System.out.println("Returning to queue");
                    this.writeMessage("Message received");
                    return true;
                default:
                    System.out.println("Invalid message from server");
                    break;
            }
        }
    }

// ---------------------------------------------------------------------------------------------------

    private void play() {
        Timer timer = new Timer();
        TimerTask timeoutTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("\nTimeout reached. Playing automatically...");
                rollDice();
            }
        };

        timer.schedule(timeoutTask, TIMEOUT);             
        this.scanner.nextLine(); // wait for user to press Enter key
        timer.cancel(); // cancel the timeout task    

        // continue with the rest of the program
        rollDice();
    }

    private void rollDice() {
        Random random = new Random();
        int play = random.nextInt(12) + 1;
        String message = "[PLAY] " + Integer.toString(play);

        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        try {
            channel.write(buffer);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// ---------------------------------------------------------------------------------------------------