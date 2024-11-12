package org.example.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.file.*;

public class AudioHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            // Đọc filePath từ client gửi lên
            String filePath = reader.readLine();

            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            // Đọc dữ liệu file âm thanh vào mảng byte
            byte[] fileData = Files.readAllBytes(path);

            // Thiết lập các header cho âm thanh (ví dụ mp3)
            exchange.getResponseHeaders().set("Content-Type", "audio/mp3");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileData.length));

            // Gửi phản hồi 200 với dữ liệu âm thanh
            exchange.sendResponseHeaders(200, fileData.length);

            // Gửi dữ liệu âm thanh cho client
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileData);
            }
        } catch (IOException e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }
}
