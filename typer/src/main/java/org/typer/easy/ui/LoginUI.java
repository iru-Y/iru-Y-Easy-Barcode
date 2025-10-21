package org.typer.easy.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

public class LoginUI extends JFrame {
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Entrar");
    private final JLabel statusLabel = new JLabel(" ");

    private static final String LOGIN_URL = "http://localhost:8080/auth/login";
    public static String jwtToken;
    public static String refreshToken;
    private static final Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);

    public LoginUI() {
        setTitle("BarcodePro - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        mainPanel.setBackground(new Color(25, 25, 25));

        JLabel title = new JLabel("BarcodePro", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(new Color(29, 164, 99));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sistema de Scanner de Código de Barras", SwingConstants.CENTER);
        subtitle.setForeground(Color.LIGHT_GRAY);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(title);
        mainPanel.add(subtitle);
        mainPanel.add(Box.createVerticalStrut(25));

        mainPanel.add(createLabeledField("E-mail", usernameField));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createLabeledField("Senha", passwordField));
        mainPanel.add(Box.createVerticalStrut(25));

        loginButton.setBackground(new Color(29, 164, 99));
        loginButton.setForeground(Color.BLACK);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setFocusPainted(false);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> doLogin());
        mainPanel.add(loginButton);

        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(statusLabel);

        add(mainPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        checkSavedToken();
    }

    private JPanel createLabeledField(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 25, 25));
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(label, BorderLayout.NORTH);
        field.setBackground(new Color(35, 35, 35));
        field.setForeground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void doLogin() {
        String email = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Preencha e-mail e senha.");
            return;
        }

        HttpURLConnection conn = null;
        try {
            String jsonInput = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
            URL url = new URL(LOGIN_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (stream == null) {
                statusLabel.setText("Sem resposta do servidor.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);

                if (status == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(response.toString());
                    jwtToken = jsonNode.get("accessToken").asText();
                    refreshToken = jsonNode.get("refreshToken").asText();

                    prefs.put("accessToken", jwtToken);
                    prefs.put("refreshToken", refreshToken);
                    statusLabel.setText("Login bem sucedido!");
                    SwingUtilities.invokeLater(() -> {
                        dispose();
                        new BarcodeTyperUI();
                    });
                } else {
                    statusLabel.setText("Falha no login: " + response);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            statusLabel.setText("Erro: " + ex.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void checkSavedToken() {
        String savedToken = prefs.get("accessToken", null);
        String savedRefresh = prefs.get("refreshToken", null);

        if (savedToken != null && !savedToken.isEmpty()) {
            jwtToken = savedToken;
            refreshToken = savedRefresh;
            statusLabel.setText("Login automático realizado.");
            SwingUtilities.invokeLater(() -> {
                dispose();
                new BarcodeTyperUI();
            });
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Falha ao aplicar tema: " + ex.getMessage());
        }
        SwingUtilities.invokeLater(LoginUI::new);
    }
}
