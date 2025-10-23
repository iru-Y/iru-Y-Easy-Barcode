package com.scanner.barcode_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;


@Component
public class AppKeepAlive {

    private static final Logger log = LoggerFactory.getLogger(AppKeepAlive.class);

    @Value("health_url")
    private String HEALTH_URL;


    @Scheduled(fixedRate = 60000)
    public void pingApp() {
        try {
            URL url = new URL(HEALTH_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status == 200 || status == 403) {
                log.debug("KeepAlive: aplicação respondendo normalmente.");
            } else {
                log.warn("KeepAlive: resposta inesperada (status {}).", status);
            }

            conn.disconnect();
        } catch (Exception e) {
            log.warn("KeepAlive: falha ao pingar aplicação: {}", e.getMessage());
        }
    }
}
