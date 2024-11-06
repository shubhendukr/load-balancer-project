package org.example.loadbalancer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HealthCheckService {
    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getName());

    private final List<Server> servers;
    private final int checkInterval;
    private final String healthCheckPath;
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public HealthCheckService(List<Server> servers, int checkInterval, String healthCheckPath) {
        this.servers = servers;
        this.checkInterval = checkInterval;
        this.healthCheckPath = healthCheckPath;
    }

    public void startHealthChecks() {
        scheduler.scheduleAtFixedRate(this::checkHealth, 0, checkInterval, TimeUnit.SECONDS);
    }

    private void checkHealth() {
        long currentTime = System.currentTimeMillis();  // Get the timestamp once for better efficiency
        for (Server server : servers) {
            boolean isHealthy = isServerHealthy(server);

            if (isHealthy != server.isHealthy()) {
                server.setHealthy(isHealthy);

                if (isHealthy) {
                    logger.log(Level.INFO, "Server {0} is back online at {1}",
                            new Object[]{server.getUrl(), formatTimestamp(currentTime)});
                } else {
                    logger.log(Level.WARNING, "Server {0} has gone offline at {1}",
                            new Object[]{server.getUrl(), formatTimestamp(currentTime)});
                }
            }
        }
    }

    private boolean isServerHealthy(Server server) {
        lock.lock();
        try {
            URL url = new URL(server.getUrl() + healthCheckPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);  // Timeout for connection attempt
            connection.setReadTimeout(2000);     // Timeout for reading the response

            int responseCode = connection.getResponseCode();

            // Log the response code for debugging purposes
            logger.log(Level.INFO, "Health check for server {0} returned response code {1}",
                    new Object[]{server.getUrl(), responseCode});

            // Determine if the server is healthy based on the response code
            boolean isHealthy = responseCode == 200;
            server.setHealthy(isHealthy);

            return isHealthy;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Health check failed for server {0}: {1}",
                    new Object[]{server.getUrl(), e.getMessage()});

            server.setHealthy(false);  // Mark the server as unhealthy in case of an exception
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void stopHealthChecks() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }
}
