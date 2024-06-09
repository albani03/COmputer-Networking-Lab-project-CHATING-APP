/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package versionSix;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JTextPane chatArea;
    private JTextField messageField;
    private int clientPort;
    private String username;
    private Map<String, List<String>> messageHistory = new HashMap<>(); // Store messages for each client

    public Client() {
        JFrame frame = new JFrame();
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        chatArea = new JTextPane();
        chatArea.setContentType("text/html"); // Set content type to HTML
        chatArea.setEditable(false);
        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
         // for right side send button and file attachment
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        
         // Load and scale the image for the select button
        ImageIcon originalIcon = new ImageIcon("C:\\Users\\User\\Downloads\\attachement.png"); // Replace with your image path
        Image scaledImage = originalIcon.getImage().getScaledInstance(40, 20, Image.SCALE_SMOOTH); // Set desired size
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        // Create the select button with the image icon
        JButton selectButton = new JButton(scaledIcon);
        selectButton.setPreferredSize(new Dimension(30, 30)); // Set button size to fit the image
        selectButton.setBorderPainted(false);
        selectButton.setContentAreaFilled(false);
        selectButton.setFocusPainted(false);
        selectButton.setOpaque(false);
        buttonPanel.add(selectButton);
        buttonPanel.add(sendButton);
        
        
        
        // Create bottom panel with BorderLayout
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(clientList), new JScrollPane(chatArea));
        splitPane.setDividerLocation(150);

        frame.setLayout(new BorderLayout());
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // send message action listener for send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        // send message action listener with input filed to work with enter button
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        clientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedClient = clientList.getSelectedValue();
                if (selectedClient != null) {
                    loadChatHistory(selectedClient.split(":")[0]);
                }
            }
        });

        String inputUsername = JOptionPane.showInputDialog(frame, "Enter your username:");
        if (inputUsername == null || inputUsername.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Username cannot be empty. Exiting.");
            System.exit(0);
        }
        username = inputUsername.trim();

        try {
            socket = new Socket("localhost", 12345);
            clientPort = socket.getLocalPort();
            frame.setTitle("Chat Client - " + username + " (" + clientPort + ")");
            frame.setVisible(true);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(username);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message;
                        while ((message = in.readLine()) != null) {
                            final String receivedMessage = message; // Make effectively final
                            if (receivedMessage.startsWith("CLIENTLIST")) {
                                final String clientListMessage = receivedMessage.substring(11); // Make effectively final
                                SwingUtilities.invokeLater(() -> {
                                    clientListModel.clear();
                                    String[] clients = clientListMessage.split(" ");
                                    for (String client : clients) {
                                        String[] clientInfo = client.split(":");
                                        if (!clientInfo[1].equals(String.valueOf(clientPort))) {
                                            clientListModel.addElement(clientInfo[0] + ":" + clientInfo[1]);
                                        }
                                    }
                                });
                            } else {
                                SwingUtilities.invokeLater(() -> {
                                    String[] parts = receivedMessage.split(": ", 2);
                                    if (parts.length == 2) {
                                        String senderUsername = parts[0];
                                        String msg = parts[1];

                                        messageHistory.computeIfAbsent(senderUsername, k -> new ArrayList<>()).add(receivedMessage);

                                        // Update chat area if the message is from the currently selected client
                                        String selectedClient = clientList.getSelectedValue();
                                        if (selectedClient != null && selectedClient.startsWith(senderUsername)) {
                                            appendToPane(chatArea, receivedMessage, Color.BLACK, senderUsername.equals(username));
                                        }
                                    }
                                });
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // Request initial client list
            out.println("CLIENTLIST");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String selectedClient = clientList.getSelectedValue();
        if (selectedClient != null) {
            String[] clientInfo = selectedClient.split(":");
            int recipientPort = Integer.parseInt(clientInfo[1]);
            String message = messageField.getText();
            out.println(recipientPort + ":" + message);
            appendToPane(chatArea, "To " + clientInfo[0] + ": " + message, Color.BLUE, true);
            messageField.setText("");

            // Store the message in the message history
            messageHistory.computeIfAbsent(clientInfo[0], k -> new ArrayList<>()).add("To " + clientInfo[0] + ": " + message);
        } else {
            JOptionPane.showMessageDialog(null, "Select a client to send a message.");
        }
    }

    private void loadChatHistory(String clientUsername) {
        chatArea.setText("");
        List<String> messages = messageHistory.getOrDefault(clientUsername, new ArrayList<>());
        for (String msg : messages) {
            appendToPane(chatArea, msg, msg.startsWith("To") ? Color.BLUE : Color.BLACK, msg.startsWith("To"));
        }
    }

//    private void appendToPane(JTextPane tp, String msg, Color c, boolean isSentByCurrentUser) {
//        String color = isSentByCurrentUser ? "#cfe9ff" : "#e9e9eb";
//        String alignment = isSentByCurrentUser ? "right" : "left";
//        String htmlMessage = String.format(
//            "<div style='text-align: %s;'><div style='display: inline-block; background-color: %s; padding: 10px; border-radius: 50px; margin: 5px; max-width: 70%%;'>%s</div></div>",
//            alignment, color, msg.trim()
//        );
//
//        // Insert the HTML message
//        HTMLDocument doc = (HTMLDocument) tp.getDocument();
//        HTMLEditorKit editorKit = (HTMLEditorKit) tp.getEditorKit();
//        try {
//            editorKit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
//        } catch (BadLocationException | IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void appendToPane(JTextPane tp, String msg, Color c, boolean isSentByCurrentUser) {
        String color = isSentByCurrentUser ? "#cfe9ff" : "#e9e9eb";
        String alignment = isSentByCurrentUser ? "right" : "left";
        String htmlMessage = String.format(
            "<div style='text-align: %s; margin: 5px;'>"
                + "<span style='display: inline-block; background-color: %s; padding: 1px; border-radius: 15px;'>%s</span>"
                + "</div>",
            alignment, color, msg.trim()
        );

        // Insert the HTML message
        HTMLDocument doc = (HTMLDocument) tp.getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) tp.getEditorKit();
        try {
            editorKit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
        } catch (BadLocationException | IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        new Client();
    }
}
