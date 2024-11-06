package org.example.loadbalancer;

public class Server {
    private final String host;
    private final int port;
    private boolean isHealthy;

    public Server (String host, int port) {
        this.host = host;
        this.port = port;
        this.isHealthy = true;  // Default to healthy when server is added
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return host + ":" + port;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean isHealthy) {
        this.isHealthy = isHealthy;
    }
}
