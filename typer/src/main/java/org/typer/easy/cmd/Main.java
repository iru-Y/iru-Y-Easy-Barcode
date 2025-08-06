package org.typer.easy.cmd;

import org.typer.easy.core.BarcodeTyper;

import java.awt.AWTException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Main {

    private static final String API_URL = "http://localhost:8080/barcode";

    public static void main(String[] args) {
        BarcodeTyper typer = new BarcodeTyper();

        try {
            List<Map<String, Object>> data = typer.fetchBarcodesFromApi(API_URL);

            if (data.isEmpty()) {
                System.err.println("ERRO: Nenhum arquivo encontrado na resposta da API.");
                System.exit(1);
            }

            List<String> filenames = data.stream()
                    .map(item -> (String) item.get("filename"))
                    .toList();

            String selectedFilename = typer.selectFilename(filenames);

            List<String> selectedBarcodes = null;
            for (Map<String, Object> item : data) {
                if (selectedFilename.equals(item.get("filename"))) {
                    selectedBarcodes = (List<String>) item.get("barcodes");
                    break;
                }
            }

            if (selectedBarcodes == null || selectedBarcodes.isEmpty()) {
                System.err.println("ERRO: Nenhum barcode encontrado para o arquivo " + selectedFilename);
                System.exit(1);
            }

            typer.typeBarcodes(selectedBarcodes);

        } catch (IOException | InterruptedException | AWTException e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
