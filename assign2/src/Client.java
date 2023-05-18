import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

public class Client {
    private static final int PORT = 5000;
    private static final int TIMEOUT = 5000;

    private SocketChannel channel;
    private User user;
    private ByteBuffer buffer;
    private Scanner scanner;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.launch();
    }

    public Client() {
        this.buffer = ByteBuffer.allocate(1024);
        this.scanner = new Scanner(System.in);
    }

    private void launch() {
        try {
            this.channel = SocketChannel.open();
            this.channel.connect(new InetSocketAddress(PORT));

            this.user = new User();
            String message = "";
            Boolean playing = true;

            while (playing) {
                message = this.readMessage();
                System.out.println("SERVER" + "-" + message);

                String[] split_message = message.split("-");
                String identifier = split_message[0];

                switch (identifier) {
                    case "INFO":
                        System.out.println(message);
                        this.writeMessage("OK");
                        break;
                    case "PLAY":
                        System.out.println(message);
                        this.play();
                        break;
                    case "EXIT":
                        System.out.println("Your updated score is " + user.getGlobalScore());
                        System.out.println("Exiting back to the queue");
                        this.writeMessage("OK");
                        // user to queue
                        playing = false;
                        break;
                    default:
                        System.out.println("Invalid message from server");
                        break;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String readMessage() throws IOException {
        this.buffer.clear();
        int bytes_read = this.channel.read(this.buffer);
        this.buffer.flip();

        if (bytes_read >= 0)
            return new String(buffer.array(), 0, bytes_read).trim();

        return "error";
    }

    public void writeMessage(String message) throws IOException {
        this.buffer.clear();
        this.buffer.put(message.getBytes());
        this.buffer.flip();
        
        this.channel.write(this.buffer);
    }

    private String[] readInput() {
        String input = this.scanner.nextLine();
        return input.split("\\s+");
    }

    private void play() {
        Timer timer = new Timer();
        TimerTask timeoutTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("\nTimeout reached. Playing automatically...");

                Random random = new Random();
                int play = random.nextInt(12) + 1;

                buffer.clear();
                buffer.put(Integer.toString(play).getBytes());
                buffer.flip();
                try {
                    channel.write(buffer);
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        timer.schedule(timeoutTask, TIMEOUT);             
        this.scanner.nextLine(); // wait for user to press Enter
        timer.cancel(); // cancel the timeout task    

        // continue with the rest of the program
        Random random = new Random();
        int play = random.nextInt(12) + 1;
        buffer.clear();
        buffer.put(Integer.toString(play).getBytes());
        buffer.flip();
        try {
            channel.write(buffer);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
