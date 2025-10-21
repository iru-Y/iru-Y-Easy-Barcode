package org.typer.easy.ui;

import org.typer.easy.core.BarcodeTyper;
import org.typer.easy.core.TyperWebSocketServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.AWTException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BarcodeTyperUI extends JFrame {
    private final JComboBox<String> fileDropdown = new JComboBox<>();
    private final JButton startButton = new JButton("Iniciar");
    private final JTextArea logArea = new JTextArea(10, 40);
    private final JRadioButton modeBatch = new JRadioButton("Modo Lote (API)");
    private final JRadioButton modeRealtime = new JRadioButton("Tempo Real (WebSocket)");

    private List<Map<String, Object>> apiResponse;
    private BarcodeTyper barcodeTyper;
    private TyperWebSocketServer wsServer;

    private static final int WS_PORT = 8025;
    private static final String API_URL = "http://localhost:8080/barcode";

    public BarcodeTyperUI() {
        setTitle("BarcodePro - Scanner");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);
        getContentPane().setBackground(new Color(25, 25, 25));

        if (LoginUI.jwtToken == null || LoginUI.jwtToken.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Você precisa fazer login primeiro!", "Erro de autenticação", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        try {
            barcodeTyper = new BarcodeTyper();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(this, "Erro ao inicializar Robot: " + e.getMessage());
            System.exit(1);
        }

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 30));
        JLabel title = new JLabel("BarcodePro - Painel de Digitação", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(29, 164, 99));
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel modePanel = new JPanel(new GridLayout(1, 2));
        modePanel.setBackground(new Color(25, 25, 25));
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(modeBatch);
        modeGroup.add(modeRealtime);
        modeBatch.setSelected(true);
        styleRadio(modeBatch);
        styleRadio(modeRealtime);
        modePanel.add(modeBatch);
        modePanel.add(modeRealtime);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBackground(new Color(25, 25, 25));
        JLabel label = new JLabel("Selecione o arquivo:");
        label.setForeground(Color.WHITE);
        centerPanel.add(label, BorderLayout.WEST);
        fileDropdown.setBackground(new Color(35, 35, 35));
        fileDropdown.setForeground(Color.WHITE);
        centerPanel.add(fileDropdown, BorderLayout.CENTER);
        styleButton(startButton);
        centerPanel.add(startButton, BorderLayout.EAST);

        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(35, 35, 35));
        logArea.setForeground(Color.WHITE);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                "Logs",
                0, 0,
                new Font("SansSerif", Font.PLAIN, 12),
                Color.LIGHT_GRAY
        ));

        JPanel body = new JPanel(new BorderLayout(10, 10));
        body.setBackground(new Color(25, 25, 25));
        body.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        body.add(modePanel, BorderLayout.NORTH);
        body.add(centerPanel, BorderLayout.CENTER);
        body.add(logScroll, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        loadApiData();
        startButton.addActionListener(this::handleStart);
    }

    private void styleRadio(JRadioButton radio) {
        radio.setForeground(Color.WHITE);
        radio.setBackground(new Color(25, 25, 25));
        radio.setFocusPainted(false);
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(29, 164, 99));
        button.setForeground(Color.BLACK);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void loadApiData() {
        try {
            apiResponse = barcodeTyper.fetchBarcodesFromApi(API_URL);
            fileDropdown.removeAllItems();
            if (apiResponse.isEmpty()) {
                log("Nenhum arquivo retornado da API.");
                startButton.setEnabled(false);
                return;
            }
            for (Map<String, Object> item : apiResponse) {
                fileDropdown.addItem((String) item.get("filename"));
            }
            log("Dados carregados com sucesso.");
        } catch (IOException | InterruptedException ex) {
            log("Erro ao buscar dados da API: " + ex.getMessage());
            startButton.setEnabled(false);
        }
    }

    private void handleStart(ActionEvent e) {
        if (LoginUI.jwtToken == null || LoginUI.jwtToken.isEmpty()) {
            log("Acesso negado: você não está autenticado.");
            return;
        }

        if (modeRealtime.isSelected()) {
            if (wsServer != null && wsServer.isRunning()) {
                log("WebSocket server já está rodando.");
                return;
            }
            wsServer = new TyperWebSocketServer(WS_PORT, barcodeTyper);
            wsServer.start();
            log("WebSocket server iniciado na porta " + WS_PORT);
        } else {
            if (wsServer != null) {
                try {
                    wsServer.stop();
                    log("WebSocket server parado.");
                } catch (InterruptedException ex) {
                    log("Erro ao parar WebSocket server: " + ex.getMessage());
                }
                wsServer = null;
            }
            log("Modo Batch iniciado...");
            String selectedFile = (String) fileDropdown.getSelectedItem();
            List<String> barcodes = apiResponse.stream()
                    .filter(item -> selectedFile.equals(item.get("filename")))
                    .findFirst()
                    .map(item -> (List<String>) item.get("barcodes"))
                    .orElse(null);

            if (barcodes == null || barcodes.isEmpty()) {
                log("Nenhum barcode encontrado para o arquivo selecionado.");
                return;
            }

            new Thread(() -> {
                try {
                    log("Começando a digitar em 5 segundos...");
                    Thread.sleep(5000);
                    barcodeTyper.typeBarcodes(barcodes);
                    log("Digitação concluída.");
                } catch (InterruptedException ex) {
                    log("Erro durante a digitação: " + ex.getMessage());
                }
            }).start();
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }
}
