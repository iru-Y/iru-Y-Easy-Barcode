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

    public String selectFilename(List<String> filenames) {
        try (var scanner = new java.util.Scanner(System.in)) {
            int selectedIndex = 0;
            while (true) {
                System.out.println("\nSelect a file using the number and press Enter:");
                for (int i = 0; i < filenames.size(); i++) {
                    String prefix = (i == selectedIndex) ? ">" : " ";
                    System.out.printf("%s %d: %s%n", prefix, i + 1, filenames.get(i));
                }
                System.out.print("Enter the file number: ");
                try {
                    int choice = Integer.parseInt(scanner.nextLine()) - 1;
                    if (choice >= 0 && choice < filenames.size()) {
                        selectedIndex = choice;
                        break;
                    }
                } catch (NumberFormatException ignored) {}
                System.out.println("Invalid input, try again.");
            }
            System.out.println("Selected file: " + filenames.get(selectedIndex));
            return filenames.get(selectedIndex);
        }
    }

    public void typeBarcodes(List<String> barcodes) throws AWTException, InterruptedException {
        Robot robot = new Robot();
        System.out.println("Place the cursor in the input field. Starting in 5 seconds...");
        Thread.sleep(5000);

        for (String barcode : barcodes) {
            typeString(robot, barcode);
            pressEnter(robot);
            Thread.sleep(1000);
        }
    }

    private void typeString(Robot robot, String text) {
        for (char c : text.toCharArray()) {
            typeChar(robot, c);
            robot.delay(50);
        }
    }

    private void typeChar(Robot robot, char character) {
        boolean isUpperCase = Character.isUpperCase(character);
        int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);

        if (keyCode == KeyEvent.VK_UNDEFINED) {
            System.err.println("Unsupported character: " + character);
            return;
        }

        if (isUpperCase) {
            robot.keyPress(KeyEvent.VK_SHIFT);
        }

        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);

        if (isUpperCase) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
    }

    private void pressEnter(Robot robot) {
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }
}
