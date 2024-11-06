# Load Balancer Project

## Project Overview

This project implements a simple round-robin load balancer in Java. The load balancer forwards incoming client requests to multiple backend servers, distributing the load among them. It also includes a health-check mechanism to monitor the status of each backend server and only routes requests to healthy servers. This setup is commonly used to improve the reliability and performance of web services by balancing the workload across multiple servers.

## Features

- **Round-Robin Load Balancing**: The load balancer distributes client requests sequentially across the backend servers.
- **Health Checks**: Periodic health checks are conducted to determine if backend servers are online. Only healthy servers receive requests.
- **Error Handling**: The load balancer returns an appropriate error message if no healthy servers are available.
- **Logging**: Logs are generated with timestamps to facilitate debugging in a distributed system.

## Prerequisites

- **Java** (version 17 or above recommended)
- **Gradle** for building the project
- **Backend servers** (These are simple servers with HTTP support to handle requests from the load balancer)

## Project Structure

- `LoadBalancer.java`: Main class that starts the load balancer and forwards requests to backend servers.
- `Server.java`: Represents a backend server and includes information such as URL and health status.
- `HealthCheckService.java`: Periodically checks the health of each backend server.

## Getting Started

### 1. Clone the Repository
```bash
git clone <this-repo-url>
cd load-balancer
```

### 2. Run the 3 Backend Servers
```bash
cd backend-server
python3 -m http.server 8081 --directory server8081
python3 -m http.server 8082 --directory server8082
python3 -m http.server 8083 --directory server8083
```

### 3.Build and Run the Load Balancer
```bash
# Build the project
./gradlew build

# Run the load balancer
java -jar build/libs/load-balancer.jar
```

The load balancer will start listening on the port specified in `LoadBalancer.java` (default: 8080).

### 4. Testing the Load Balancer
To test load balancing, send multiple requests to the load balancer’s port and observe how it distributes them across the backend servers.

#### 4.1 Basic Load Balancing Test
To test load balancing, send multiple requests to the load balancer’s port and observe how it distributes them across the backend servers.
```bash
curl http://localhost:8080
```
Run this multiple times to see that requests are routed to each backend server in a round-robin fashion.

#### 4.2 No Healthy Servers Available
To test this scenario, stop all backend servers:
```bash
# Stop servers on ports 8081, 8082, and 8083
# Ctrl+C or close the terminals where they are running
```
Now, send a request to the load balancer. You should receive a 503 Service Unavailable error message indicating that no backend servers are available.

Example:
```bash
curl http://localhost:8080
# Expected output: 503 Service Unavailable
```

This `README.md` includes an overview, setup instructions, testing methods, logging information, and suggestions for future improvements. Let me know if you'd like to add anything else!
