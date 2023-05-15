import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.InetSocketAddress;

public class Server {
    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 3;

    private Selector selector;
    private List<Client> clients;
    private ExecutorService executor;

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    public Server() {
        clients = new ArrayList<>();
        executor = Executors.newFixedThreadPool(MAX_CLIENTS);
    }

    public void start() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server is listening on port " + PORT);

            while (true) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }

                if (this.clients.size() == MAX_CLIENTS) {
                    for (Client client : clients) {
                        SelectionKey key = client.getChannel().keyFor(selector);
                        if (key != null)
                            key.cancel();
                    }
                    Game game = new Game(this.clients);
                    executor.submit(game);
                }
            }
        } catch (IOException ex) {
            System.err.println("Server exception: " + ex.getMessage());
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);

        Client newClient = new Client(clientChannel);
        clients.add(newClient);

        System.out.println("New client connected: " + clientChannel.getRemoteAddress());
        newClient.sendMessage("Waiting for the game to begin...\n");
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // Connection closed by client
            Client disconnectedClient = findClientByChannel(clientChannel);
            clients.remove(disconnectedClient);
            clientChannel.close();
            System.out.println("Client disconnected: " + clientChannel.getRemoteAddress() + "\n");
            return;
        }

        String message = new String(buffer.array(), 0, bytesRead).trim();
        Client currentClient = findClientByChannel(clientChannel);

        if (currentClient != null)
            System.out.println("Received message from client " + clientChannel.getRemoteAddress() + ": " + message + "\n");
    }

    private Client findClientByChannel(SocketChannel clientChannel) {
        for (Client client : clients)
            if (client.getChannel() == clientChannel)
                return client;

        return null;
    }
}