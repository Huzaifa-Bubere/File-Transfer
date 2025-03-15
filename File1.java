import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class File1 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("File Transfer App");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLayout(new BorderLayout());

            // Create a menu bar
            JMenuBar menuBar = new JMenuBar();
            JMenu menu = new JMenu("File");
            JMenuItem startServerMenuItem = new JMenuItem("Start Server");
            JMenuItem connectClientMenuItem = new JMenuItem("Connect Client");

            // Add action listeners for menu items
            startServerMenuItem.addActionListener(e -> new Thread(File1::startServer).start());
            connectClientMenuItem.addActionListener(e -> {
                String serverIP = showCustomInputDialog("Enter the server IP address:");
                new Thread(() -> connectClient(serverIP)).start();
            });

            menu.add(startServerMenuItem);
            menu.add(connectClientMenuItem);
            menuBar.add(menu);
            frame.setJMenuBar(menuBar);

            // Create buttons for client and server with rounded borders and gradient background
            JButton startServerButton = createCustomButton("Start Server", new Color(0, 204, 255), new Color(102, 255, 255));
            JButton connectClientButton = createCustomButton("Connect Client", new Color(153, 255, 102), new Color(51, 204, 51));

            // Add action listeners to the buttons
            startServerButton.addActionListener(e -> new Thread(File1::startServer).start());
            connectClientButton.addActionListener(e -> {
                String serverIP = showCustomInputDialog("Enter the server IP address:");
                new Thread(() -> connectClient(serverIP)).start();
            });

            // Add buttons to the panel
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBorder(new EmptyBorder(30, 30, 30, 30)); // Padding for the button panel
            buttonPanel.setLayout(new GridLayout(2, 1, 10, 10)); // Layout with spacing
            buttonPanel.add(startServerButton);
            buttonPanel.add(connectClientButton);

            frame.add(buttonPanel, BorderLayout.CENTER);

            // Customize the overall look of the frame
            frame.getContentPane().setBackground(new Color(230, 230, 250)); // Light background
            frame.setVisible(true);
        });
    }

    // Method to create a custom button with gradient and rounded borders
    private static JButton createCustomButton(String text, Color color1, Color color2) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create a gradient background for the button
                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Rounded corners

                // Paint text (after the background gradient)
                super.paintComponent(g);
            }
        };

        button.setPreferredSize(new Dimension(200, 50)); // Set button width and height
        button.setContentAreaFilled(false); // Disable default background
        button.setOpaque(false); // Transparent background for gradient
        button.setForeground(Color.WHITE); // Set text color
        button.setFont(new Font("Arial", Font.BOLD, 16)); // Set custom font
        button.setFocusPainted(false); // Remove focus border
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding inside the button

        // Add hover effect using MouseAdapter
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2)); // White border on hover
            }

            public void mouseExited(MouseEvent evt) {
                button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Reset border when not hovering
            }
        });

        return button;
    }

    // Method to display a custom input dialog
    private static String showCustomInputDialog(String message) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel(message);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        JTextField textField = new JTextField(15);
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        // Custom dialog design
        int result = JOptionPane.showConfirmDialog(null, panel, "Input", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            return textField.getText();
        } else {
            return null;
        }
    }

    // Method to show a custom message dialog
    private static void showCustomMessageDialog(String message, String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel(message);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(label, BorderLayout.CENTER);

        // Show the custom dialog
        JOptionPane.showMessageDialog(null, panel, title, JOptionPane.PLAIN_MESSAGE);
    }

    // Modify the startServer method to support multiple clients and ask for file path only once
    private static void startServer() {
        ServerSocket serverSocket = null;
        File file = null;

        try {
            serverSocket = new ServerSocket(5000);
            System.out.println("Server is waiting for clients...");

            // Ask for the file path once when the server starts
            String filePath = showCustomInputDialog("Enter the full path of the file to send:");
            file = new File(filePath);

            // Check if the file exists
            if (!file.exists()) {
                showCustomMessageDialog("File not found!", "Error");
                return;
            }

            // Continuously accept clients in a loop
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected!");

                // Handle each client in a new thread, passing the same file to all clients
                new Thread(new ClientHandler(socket, file)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Modified ClientHandler class to take the file as a parameter
    static class ClientHandler implements Runnable {
        private Socket socket;
        private File file;

        public ClientHandler(Socket socket, File file) {
            this.socket = socket;
            this.file = file;
        }

        @Override
        public void run() {
            DataOutputStream dos = null;
            FileInputStream fis = null;
            BufferedInputStream bis = null;

            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                dos = new DataOutputStream(socket.getOutputStream());

                // Send file information
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                dos.flush();

                // Send file data
                byte[] buffer = new byte[64*1024];
                long totalBytesSent = 0;
                int bytesRead;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalBytesSent += bytesRead;
                }

                dos.flush(); // Ensure all data is sent
                System.out.println("File sent successfully! Total bytes sent: " + totalBytesSent);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bis != null) bis.close();
                    if (fis != null) fis.close();
                    if (dos != null) dos.close();
                    if (socket != null) socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    // Method to connect the client
    private static void connectClient(String serverIP) {
        Socket socket = null;
        DataInputStream dis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        try {
            socket = new Socket(serverIP, 5000);
            System.out.println("Connected to the server!");

            dis = new DataInputStream(socket.getInputStream());

            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            String saveDir = showCustomInputDialog("Enter the directory to save the file:");
            File receivedFile = new File(saveDir + "/" + fileName);

            fos = new FileOutputStream(receivedFile);
            bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[64*1024];
            long totalBytesRead = 0;
            int bytesRead;

            while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            bos.flush(); // Ensure all data is written to the file
            System.out.println("File received successfully! Total bytes received: " + totalBytesRead);

            // Check if the received file size matches the expected size
            if (totalBytesRead == fileSize) {
                showCustomMessageDialog("File received successfully!", "Success");
            } else {
                showCustomMessageDialog("File transfer incomplete!", "Error");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) bos.close();
                if (fos != null) fos.close();
                if (dis != null) dis.close();
                if (socket != null) socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
