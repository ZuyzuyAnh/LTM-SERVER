package org.example.server;

import com.sun.net.httpserver.*;
import org.example.dao.QuestionDAO;
import org.example.model.Question;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUploadHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                exchange.sendResponseHeaders(400, 0);  // Bad Request nếu không phải multipart
                return;
            }

            String boundary = "--" + contentType.split("boundary=")[1];
            InputStream inputStream = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String line;
            boolean isFilePart = false;
            FileOutputStream fileOutputStream = null;
            String filePath = "";

            while ((line = reader.readLine()) != null) {
                // Kiểm tra dòng Content-Disposition để xác định phần dữ liệu file
                if (line.contains("Content-Disposition: form-data; name=\"file\"")) {
                    // Tạo thư mục và file để lưu trữ
                    File audioDir = new File("audio");
                    if (!audioDir.exists()) {
                        audioDir.mkdirs();
                    }
                    File outputFile = new File(audioDir, "audio_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".wav");
                    filePath = outputFile.getAbsolutePath();
                    fileOutputStream = new FileOutputStream(outputFile);

                    // Bỏ qua các dòng header của phần file
                    while (!(line = reader.readLine()).isEmpty()) {}

                    isFilePart = true;
                    continue;
                }

                // Ghi nội dung file nếu đang ở phần tệp
                if (isFilePart) {
                    if (line.contains(boundary)) {
                        // Đóng file khi gặp boundary mới (kết thúc file)
                        fileOutputStream.close();
                        isFilePart = false;
                    } else {
                        fileOutputStream.write(line.getBytes(StandardCharsets.UTF_8));
                        fileOutputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                    }
                }
            }

            // Trả về đường dẫn của file sau khi upload thành công
            String response = filePath;
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

    }
}
