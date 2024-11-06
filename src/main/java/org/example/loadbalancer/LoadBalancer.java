package org.example.loadbalancer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadBalancer {
    private static final Logger logger = Logger.getLogger(LoadBalancer.class.getName());

    private static final int LOAD_BALANCER_PORT = 8080;
    private static final String BACKEND_HOST = "http://localhost";
    private static final int PORT1 = 8081;
    private static final int PORT2 = 8082;
    private static final int PORT3 = 8083;

    private static final List<Server> servers = new ArrayList<>();
    private final HealthCheckService healthCheckService;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();

    LoadBalancer(List<Server> servers, int healthCheckInterval, String healthCheckPath) {
        this.healthCheckService = new HealthCheckService(servers, healthCheckInterval, healthCheckPath);
        healthCheckService.startHealthChecks();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(LOAD_BALANCER_PORT)) {
            logger.log(Level.INFO, "Load Balancer started on port: {0}", LOAD_BALANCER_PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    logger.log(Level.INFO, "Received connection from: {0}", clientSocket.getInetAddress());

                    Server server = getNextHealthyServer();
                    if (server == null) {
                        sendError(clientSocket, "503 Service Unavailable",
                                "No healthy servers available to handle the request.");
                        continue;
                    }

                    forwardRequest(clientSocket, server);
                }
            }
        } finally {
            healthCheckService.stopHealthChecks();
        }
    }

    private Server getNextHealthyServer() {
        lock.lock();
        try {
            for (int i = 0; i < servers.size(); i++) {
                int index = currentIndex.getAndUpdate(val -> (val + 1) % servers.size());
                Server server = servers.get(index);
                if (server.isHealthy()) {
                    logger.log(Level.INFO, "Routing request to server: {0}", server.getUrl());
                    return server;
                }
            }
        } finally {
            lock.unlock();
        }

        logger.log(Level.SEVERE, "No healthy servers available at {0}", formatTimestamp(System.currentTimeMillis()));
        return null; // No healthy server found
    }

    private void forwardRequest(Socket clientSocket, Server server) {
        int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetries && !success) {
            try (Socket serverSocket = new Socket()) {
                // Parse the server URL to get the hostname and port
                URL url = new URL(server.getUrl());
                String serverHost = url.getHost();  // Get host from the URL (e.g., "localhost")
                int serverPort = url.getPort() == -1 ? 80 : url.getPort();  // Default to 80 if no port is specified

                // Log the connection attempt for debugging
                logger.log(Level.INFO, "Connecting to server {0}:{1}", new Object[]{serverHost, serverPort});

                // Attempt to connect to the backend server with a timeout
                serverSocket.connect(new InetSocketAddress(serverHost, serverPort), 2000); // 2-second timeout

                InputStream clientIn = clientSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();
                InputStream serverIn = serverSocket.getInputStream();
                OutputStream serverOut = serverSocket.getOutputStream();

                // Forward request from client to server
                byte[] requestBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = clientIn.read(requestBuffer)) != -1) {
                    serverOut.write(requestBuffer, 0, bytesRead);
                }
                serverOut.flush();

                // Forward response from server to client
                byte[] responseBuffer = new byte[1024];
                while ((bytesRead = serverIn.read(responseBuffer)) != -1) {
                    clientOut.write(responseBuffer, 0, bytesRead);
                }
                clientOut.flush();

            } catch (IOException e) {
                // Detailed error logging
                logger.log(Level.WARNING, "Error forwarding request to server {0}: {1}", new Object[]{server.getUrl(), e.toString()});
                attempt++; // Log full stack trace for debugging
            }
        }

        if (!success) {
            sendError(clientSocket, "503 Service Unavailable",
                    "The load balancer could not connect to the backend server.");
        }
    }

    private void sendError(Socket clientSocket, String status, String message) {
        try (OutputStream clientOut = clientSocket.getOutputStream()) {
            String errorResponse = "HTTP/1.1 " + status + "\r\n\r\n" + message;
            clientOut.write(errorResponse.getBytes());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error sending error response: {0}", e.getMessage());
        }
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    public static void main(String[] args) throws IOException {
        // Add backend servers
        servers.add(new Server(BACKEND_HOST, PORT1));
        servers.add(new Server(BACKEND_HOST, PORT2));
        servers.add(new Server(BACKEND_HOST, PORT3));

        int healthCheckInterval = 10; // seconds
        String healthCheckPath = "/";

        LoadBalancer loadBalancer = new LoadBalancer(servers, healthCheckInterval, healthCheckPath);
        loadBalancer.start();
    }
}
