package org.typer.easy.ui;

import org.typer.easy.core.BarcodeTyper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final JComboBox<String> exportFormat = new JComboBox<>(new String[]{"PDF", "TXT", "CSV"});
    private final JButton exportButton = new JButton("Baixar lista");

    private List<Map<String, Object>> apiResponse;
    private BarcodeTyper barcodeTyper;
    private final AtomicBoolean stopTyping = new AtomicBoolean(false);

    private static final String API_URL = "http://localhost:8080/barcode";
    private static final Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);

    private TrayIcon trayIcon;

    public BarcodeTyperUI() {
        setTitle("BarcodePro - Scanner");
        setDefaultCloseOperation(HIDE_ON_CLOSE); // <== importante para tray
        setLayout(new BorderLayout(10, 10));
        setResizable(true);
        setMinimumSize(new Dimension(800, 600));
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

        initTray(); // inicializa o tray

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 30));
        JLabel title = new JLabel("BarcodePro - Painel de Digitação", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(new Color(29, 164, 99));
        styleButton(logoutButton);
        logoutButton.addActionListener(this::handleLogout);
        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        headerButtons.setBackground(new Color(30, 30, 30));
        headerButtons.add(logoutButton);
        header.add(title, BorderLayout.CENTER);
        header.add(headerButtons, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(25, 25, 25));
        JLabel label = new JLabel("Selecione o arquivo:");
        label.setForeground(Color.WHITE);
        centerPanel.add(label);
        fileDropdown.setBackground(new Color(35, 35, 35));
        fileDropdown.setForeground(Color.WHITE);
        fileDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
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
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(buttonPanel);

        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(35, 35, 35));
        logArea.setForeground(new Color(29, 164, 99));
        JScrollPane logScroll = new JScrollPane(logArea);

        JPanel body = new JPanel(new BorderLayout(10, 10));
        body.setBackground(new Color(25, 25, 25));
        body.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        body.add(centerPanel, BorderLayout.CENTER);
        body.add(logScroll, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);

        // Listener global de teclado para ESC e Ctrl+C
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE ||
                        (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown())) {
                    if (!stopTyping.get()) {
                        stopTyping.set(true);
                        log("⚠️ Digitação automática interrompida pelo usuário!");
                    }
                }
            }
            return false;
        });

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

        // Ao fechar janela, envia para tray
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                log("Aplicativo minimizado para tray.");
            }
        });
    }

    private void initTray() {
        if (!SystemTray.isSupported()) return;

        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")); // ícone do tray
        PopupMenu popup = new PopupMenu();

        MenuItem openItem = new MenuItem("Abrir");
        openItem.addActionListener(e -> SwingUtilities.invokeLater(() -> setVisible(true)));
        MenuItem exitItem = new MenuItem("Sair");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            System.exit(0);
        });

        popup.add(openItem);
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "BarcodePro", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // duplo clique abre a janela
                if (e.getClickCount() == 2) SwingUtilities.invokeLater(() -> setVisible(true));
            }
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log("Erro ao adicionar tray icon: " + e.getMessage());
        }
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

        String selectedFile = (String) fileDropdown.getSelectedItem();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Selecione um arquivo primeiro!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                """
                A digitação automática começará em 5 segundos.

                ⚠️ Para interromper a digitação:
                - Pressione ESC
                - Ou pressione CTRL + C

                Deseja continuar?
                """,
                "Aviso de segurança",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        stopTyping.set(false); // reset antes de iniciar

        log("Modo Batch iniciado...");
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

                List<String> partial = new java.util.ArrayList<>();
                for (String b : barcodes) {
                    if (stopTyping.get()) {
                        log("⚠️ Processo de digitação interrompido!");
                        return;
                    }
                    partial.clear();
                    partial.add(b);
                    barcodeTyper.typeBarcodes(partial);
                    Thread.sleep(100);
                }

                log("✅ Digitação concluída.");
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
            case "TXT" -> Files.write(out, barcodes, StandardCharsets.UTF_8);
            case "CSV" -> {
                List<String> lines = new java.util.ArrayList<>();
                lines.add("barcode");
                lines.addAll(barcodes);
                Files.write(out, lines, StandardCharsets.UTF_8);
            }
            case "PDF" -> {
                try (PDDocument doc = new PDDocument()) {
                    PDPage page = new PDPage();
                    doc.addPage(page);
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 12);
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
            }
            default -> throw new IOException("Formato desconhecido: " + format);
        }

        log("Arquivo exportado: " + out.toAbsolutePath());
        JOptionPane.showMessageDialog(this, "Exportado com sucesso: " + out.toAbsolutePath(), "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }
}
