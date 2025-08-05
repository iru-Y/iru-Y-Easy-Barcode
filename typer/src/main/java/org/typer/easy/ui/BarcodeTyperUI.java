package org.typer.easy.ui;

import org.typer.easy.core.BarcodeTyper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.AWTException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BarcodeTyperUI extends JFrame {

    private final JComboBox<String> fileDropdown = new JComboBox<>();
    private final JButton startButton = new JButton("Start Typing");
    private final JTextArea logArea = new JTextArea(10, 40);

    private List<Map<String, Object>> apiResponse;
    private final BarcodeTyper barcodeTyper = new BarcodeTyper();
    private static final String API_URL = "http://localhost:8080/barcode";

    public BarcodeTyperUI() {
        setTitle("Easy Barcode Typer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(new JLabel("Select file:"), BorderLayout.WEST);
        topPanel.add(fileDropdown, BorderLayout.CENTER);
        topPanel.add(startButton, BorderLayout.EAST);

        JScrollPane logScroll = new JScrollPane(logArea);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setLineWrap(true);

        add(topPanel, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        loadApiData();

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStartTyping();
            }
        });
    }

    private void loadApiData() {
        try {
            apiResponse = barcodeTyper.fetchBarcodesFromApi(API_URL);
            if (apiResponse.isEmpty()) {
                log("No files found from API.");
                startButton.setEnabled(false);
                return;
            }

            for (Map<String, Object> item : apiResponse) {
                fileDropdown.addItem((String) item.get("filename"));
            }

            log("Data loaded successfully.");

        } catch (IOException | InterruptedException ex) {
            log("Error fetching data from API: " + ex.getMessage());
            startButton.setEnabled(false);
        }
    }

    private void handleStartTyping() {
        String selectedFile = (String) fileDropdown.getSelectedItem();
        List<String> barcodes = apiResponse.stream()
                .filter(item -> selectedFile.equals(item.get("filename")))
                .findFirst()
                .map(item -> (List<String>) item.get("barcodes"))
                .orElse(null);

        if (barcodes == null || barcodes.isEmpty()) {
            log("No barcodes found for selected file.");
            return;
        }

        log("Typing will start in 5 seconds...");
        new Thread(() -> {
            try {
                barcodeTyper.typeBarcodes(barcodes);
                log("Typing completed.");
            } catch (AWTException | InterruptedException ex) {
                log("Error during typing: " + ex.getMessage());
            }
        }).start();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }
}
