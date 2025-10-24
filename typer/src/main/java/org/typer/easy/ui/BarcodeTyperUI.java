package org.typer.easy.ui;

import org.typer.easy.core.BarcodeTyper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.AWTException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class BarcodeTyperUI extends JFrame {
    private final JComboBox<String> fileDropdown = new JComboBox<>();
    private final JButton startButton = new JButton("Iniciar");
    private final JButton reloadButton = new JButton("Recarregar");
    private final JButton logoutButton = new JButton("Logout");
    private final JTextArea logArea = new JTextArea(10, 40);

    // export UI
    private final JComboBox<String> exportFormat = new JComboBox<>(new String[]{"PDF", "TXT", "CSV"});
    private final JButton exportButton = new JButton("Baixar lista");

    private List<Map<String, Object>> apiResponse;
    private BarcodeTyper barcodeTyper;

    private static final String API_URL = "http://localhost:8080/barcode";
    private static final Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);

    public BarcodeTyperUI() {
        setTitle("BarcodePro - Scanner");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);
        getContentPane().setBackground(new Color(25, 25, 25));

        if (LoginUI.jwtToken == null || LoginUI.jwtToken.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Você precisa fazer login primeiro!", "Erro de autenticação", JOptionPane.ERROR_MESSAGE);
            dispose();
            SwingUtilities.invokeLater(LoginUI::new);
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

        styleButton(logoutButton);
        logoutButton.setText("Logout");
        logoutButton.addActionListener(this::handleLogout);

        header.add(title, BorderLayout.CENTER);
        header.add(logoutButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(25, 25, 25));

        JLabel label = new JLabel("Selecione o arquivo:");
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(label);

        fileDropdown.setBackground(new Color(35, 35, 35));
        fileDropdown.setForeground(Color.WHITE);
        fileDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 15));
        fileDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(Box.createVerticalStrut(5));
        centerPanel.add(fileDropdown);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(new Color(25, 25, 25));
        styleButton(reloadButton);
        styleButton(startButton);
        styleButton(exportButton);
        buttonPanel.add(reloadButton);
        buttonPanel.add(startButton);
        buttonPanel.add(exportFormat);
        buttonPanel.add(exportButton);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(buttonPanel);

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
        body.add(centerPanel, BorderLayout.CENTER);
        body.add(logScroll, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        loadApiData();
        startButton.addActionListener(this::handleStart);
        reloadButton.addActionListener(e -> {
            log("Recarregando dados da API...");
            loadApiData();
        });

        exportButton.addActionListener(e -> {
            String format = (String) exportFormat.getSelectedItem();
            try {
                exportSelectedList(format);
            } catch (IOException ex) {
                log("Erro ao exportar: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
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

            if (apiResponse == null || apiResponse.isEmpty()) {
                log("Nenhum arquivo retornado da API.");
                startButton.setEnabled(false);
                exportButton.setEnabled(false);
                return;
            }

            for (Map<String, Object> item : apiResponse) {
                fileDropdown.addItem((String) item.get("filename"));
            }

            startButton.setEnabled(true);
            exportButton.setEnabled(true);
            log("Dados carregados com sucesso.");
        } catch (IOException | InterruptedException ex) {
            log("Erro ao buscar dados da API: " + ex.getMessage());
            startButton.setEnabled(false);
            exportButton.setEnabled(false);
        }
    }

    private void handleStart(ActionEvent e) {
        if (LoginUI.jwtToken == null || LoginUI.jwtToken.isEmpty()) {
            log("Acesso negado: você não está autenticado.");
            return;
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

    private void handleLogout(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente sair?",
                "Confirmação de Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            LoginUI.jwtToken = null;
            LoginUI.refreshToken = null;
            prefs.remove("accessToken");
            prefs.remove("refreshToken");

            dispose();
            SwingUtilities.invokeLater(LoginUI::new);
        }
    }

    private void exportSelectedList(String format) throws IOException {
        String selectedFile = (String) fileDropdown.getSelectedItem();
        if (selectedFile == null) {
            log("Nenhum arquivo selecionado para exportar.");
            JOptionPane.showMessageDialog(this, "Selecione um arquivo antes de exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> barcodes = apiResponse.stream()
                .filter(item -> selectedFile.equals(item.get("filename")))
                .findFirst()
                .map(item -> (List<String>) item.get("barcodes"))
                .orElse(null);

        if (barcodes == null || barcodes.isEmpty()) {
            log("Nenhum barcode encontrado para o arquivo selecionado.");
            JOptionPane.showMessageDialog(this, "Nenhum barcode encontrado para o arquivo selecionado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ext = format.toLowerCase();
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(selectedFile + "." + ext));
        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        Path out = chooser.getSelectedFile().toPath();

        switch (format) {
            case "TXT":
                Files.write(out, barcodes, StandardCharsets.UTF_8);
                break;
            case "CSV":
                // simple CSV with header
                List<String> lines = new java.util.ArrayList<>();
                lines.add("barcode");
                for (String b : barcodes) lines.add(b);
                Files.write(out, lines, StandardCharsets.UTF_8);
                break;
            case "PDF":
                // Uses Apache PDFBox
                try (PDDocument doc = new PDDocument()) {
                    PDPage page = new PDPage();
                    doc.addPage(page);

                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 12);
                        // start near top-left
                        float margin = 50;
                        float y = 750;
                        cs.newLineAtOffset(margin, y);

                        for (String b : barcodes) {
                            cs.showText(b);
                            cs.newLineAtOffset(0, -15);
                        }

                        cs.endText();
                    }

                    doc.save(out.toFile());
                }
                break;
            default:
                throw new IOException("Formato desconhecido: " + format);
        }

        log("Arquivo exportado: " + out.toAbsolutePath());
        JOptionPane.showMessageDialog(this, "Exportado com sucesso: " + out.toAbsolutePath(), "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }
}
