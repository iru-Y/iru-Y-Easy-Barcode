package org.typer.easy.core;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.io.IOException;

public class TyperWebSocketServer extends WebSocketServer {

    private final BarcodeTyper barcodeTyper;
    private boolean running = false;

    public TyperWebSocketServer(int port, BarcodeTyper barcodeTyper) {
        super(new InetSocketAddress(port));
        this.barcodeTyper = barcodeTyper;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Cliente conectado: " + conn.getRemoteSocketAddress());
        conn.send("Conexão estabelecida no servidor Typer.");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Cliente desconectado: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Código recebido via WebSocket: " + message);
        if (barcodeTyper != null) {
            barcodeTyper.typeString(message);
            barcodeTyper.pressEnter();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        running = true;
        System.out.println("Servidor WebSocket iniciado!");
    }

    @Override
    public void stop() throws InterruptedException {
        super.stop();
        running = false;
        System.out.println("Servidor WebSocket parado!");
    }

    public boolean isRunning() {
        return running;
    }
}
