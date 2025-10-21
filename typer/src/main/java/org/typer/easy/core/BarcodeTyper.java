package org.typer.easy.core;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.typer.easy.ui.LoginUI;

public class BarcodeTyper {

    private final Robot robot;

    public BarcodeTyper() throws AWTException {
        this.robot = new Robot();
    }

    public List<Map<String, Object>> fetchBarcodesFromApi(String apiUrl) throws IOException, InterruptedException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + LoginUI.jwtToken);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (stream == null) {
                throw new IOException("Sem resposta do servidor");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);

                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(response.toString(), List.class);
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }


    public void typeBarcodes(List<String> barcodes) throws InterruptedException {
        System.out.println("Posicione o cursor no campo de texto. Iniciando em 5 segundos...");
        Thread.sleep(5000);

        for (String barcode : barcodes) {
            typeString(barcode);
            pressEnter();
            Thread.sleep(1000);
        }
    }

    public void typeString(String text) {
        for (char c : text.toCharArray()) {
            typeChar(c);
            robot.delay(50);
        }
    }

    public void typeChar(char character) {
        boolean isUpperCase = Character.isUpperCase(character);
        int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);

        if (keyCode == KeyEvent.VK_UNDEFINED) {
            System.err.println("Unsupported character: " + character);
            return;
        }

        if (isUpperCase) robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        if (isUpperCase) robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    public void pressEnter() {
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }
}
