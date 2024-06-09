/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package versionSix;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

public class Server {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private Map<Integer, ClientHandler> clientHandlers = new HashMap<>();
    private Map<Integer, String> clientUsernames = new HashMap<>();
    private DefaultListModel<String> clientListModel;
    private JTextArea logArea;

    public Server() {
        JFrame frame = new JFrame("Server");
        clientListModel = new DefaultListModel<>();
        JList<String> clientList = new JList<>(clientListModel);
        logArea = new JTextArea();
        logArea.setEditable(false);

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(clientList), BorderLayout.WEST);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        try {
            serverSocket = new ServerSocket(PORT);
            log("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientPort = clientSocket.getPort();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            log("Error: " + e.getMessage());
        }
    }

    public synchronized void addClient(int clientPort, ClientHandler handler, String username) {
        clientHandlers.put(clientPort, handler);
        clientUsernames.put(clientPort, username);
        clientListModel.addElement(username);
        log("Client connected: " + username);
        broadcastClientList();
    }

    public synchronized void removeClient(int clientPort) {
        String username = clientUsernames.remove(clientPort);
        clientHandlers.remove(clientPort);
        clientListModel.removeElement(username);
        log("Client disconnected: " + username);
        broadcastClientList();
    }

    public synchronized void log(String message) {
        logArea.append(message + "\n");
    }

    public synchronized void sendMessageToClient(int clientPort, String message) {
        ClientHandler handler = clientHandlers.get(clientPort);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    public synchronized void broadcastClientList() {
        StringBuilder clients = new StringBuilder("CLIENTLIST ");
        for (Map.Entry<Integer, String> entry : clientUsernames.entrySet()) {
            clients.append(entry.getValue()).append(":").append(entry.getKey()).append(" ");
        }
        String clientListStr = clients.toString().trim();
        for (ClientHandler handler : clientHandlers.values()) {
            handler.sendMessage(clientListStr);
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}



class ClientHandler implements Runnable {
    private Socket socket;
    private Server server;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            username = in.readLine();
            server.addClient(socket.getPort(), this, username);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("CLIENTLIST")) {
                    server.broadcastClientList();
                } else {
                    String[] parts = message.split(":", 2);
                    if (parts.length == 2) {
                        int recipientPort = Integer.parseInt(parts[0]);
                        String msg = parts[1];
                        String formattedMessage = username + ": " + msg;
                        server.sendMessageToClient(recipientPort, formattedMessage);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.removeClient(socket.getPort());
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
