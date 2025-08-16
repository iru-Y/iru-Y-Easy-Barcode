package org.typer.easy.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final JButton loginButton = new JButton("Login");
    private final JLabel statusLabel = new JLabel(" ");

    private static final String LOGIN_URL = "http://localhost:8080/auth/login";

    public static String jwtToken;
    public static String refreshToken;

    private static final Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);

    public LoginUI() {
        setTitle("Login - Easy Barcode Typer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.add(new JLabel("Usuário:"));
        formPanel.add(usernameField);
        formPanel.add(new JLabel("Senha:"));
        formPanel.add(passwordField);
        formPanel.add(new JLabel(""));
        formPanel.add(loginButton);

        add(formPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> doLogin());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        checkSavedToken();
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

            System.out.println("POST " + LOGIN_URL);
            System.out.println("Request body: " + jsonInput);
            System.out.println("=== Request Headers ===");
            conn.getRequestProperties().forEach((k, v) -> System.out.println(k + ": " + v));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            System.out.println("=== Response Status ===");
            System.out.println("HTTP " + status);

            InputStream stream;
            if (status >= 200 && status < 300) {
                stream = conn.getInputStream();
            } else {
                stream = conn.getErrorStream();
            }

            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    System.out.println("=== Response Body ===");
                    System.out.println(response);

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
                        statusLabel.setText("Falha no login: " + response.toString());
                    }
                }
            } else {
                statusLabel.setText("Erro inesperado: sem resposta do servidor (HTTP " + status + ")");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            statusLabel.setText("Erro: " + ex.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginUI::new);
    }
}
