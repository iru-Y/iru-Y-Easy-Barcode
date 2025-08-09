package org.typer.easy.core;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BarcodeTyper {

    private final Robot robot;

    public BarcodeTyper() throws AWTException {
        this.robot = new Robot();
    }

    public List<Map<String, Object>> fetchBarcodesFromApi(String apiUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Error fetching data from API: Status code " + response.statusCode());
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
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
