package org.kingdoms.utils.network;

import com.google.gson.JsonElement;
import org.kingdoms.utils.gson.KingdomsGson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Can work with strings with up to 2,147,483,647 in length using 4 bytes.
 */
public abstract class SocketJsonCommunicator {
    private final ServerSocket serverSocket;
    private Socket socket;
    private final Thread dataListener;
    private final Logger logger;

    public SocketJsonCommunicator(int port, Logger logger) {
        this(null, port, logger); // Localhost
    }

    public SocketJsonCommunicator(InetAddress ip, int port, Logger logger) {
        this.logger = logger;
        try {
            serverSocket = new ServerSocket(port, 10, ip);
            log("Socket server started.");
            log("Creating data listener thread...");
            dataListener = new Thread(new Listener());
            dataListener.start();
            log("Listener thread has started.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void log(String info) {
        logger.info("[Socket][" + serverSocket.getInetAddress() + ':' + serverSocket.getLocalPort() + "] " + info);
    }

    public abstract void onReceive(JsonElement data);

    private final class Listener extends Thread {
        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            try {
                while (true) {
                    log("Accepting socket data...");
                    socket = serverSocket.accept();
                    log("Reading received data...");
                    try {
                        JsonElement data = receive();
                        onReceive(data);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public JsonElement receive() {
        try {
            InputStream is = socket.getInputStream();
            byte[] lenBytes = new byte[4];
            is.read(lenBytes, 0, 4);
            int len = (((lenBytes[3] & 0xff) << 24) | ((lenBytes[2] & 0xff) << 16) |
                    ((lenBytes[1] & 0xff) << 8) | (lenBytes[0] & 0xff));
            byte[] receivedBytes = new byte[len];
            is.read(receivedBytes, 0, len);
            String data = new String(receivedBytes, 0, len);
            log("Received: " + data);
            return KingdomsGson.fromString(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(JsonElement data) {
        try (OutputStream os = socket.getOutputStream()) {
            // TODO: Somehow disable pretty-printing? But we need a new GSON instance...
            String jsonStr = KingdomsGson.toString(data);
            log("Sending: " + jsonStr);

            byte[] toSendBytes = jsonStr.getBytes();
            int toSendLen = toSendBytes.length;
            byte[] toSendLenBytes = new byte[4];
            toSendLenBytes[0] = (byte) (toSendLen & 0xff);
            toSendLenBytes[1] = (byte) ((toSendLen >> 8) & 0xff);
            toSendLenBytes[2] = (byte) ((toSendLen >> 16) & 0xff);
            toSendLenBytes[3] = (byte) ((toSendLen >> 24) & 0xff);
            os.write(toSendLenBytes);
            os.write(toSendBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
