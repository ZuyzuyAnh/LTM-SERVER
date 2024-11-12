package org.example.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Base64ToMp3Converter {

    /**
     * Phương thức để lưu chuỗi base64 thành file MP3.
     *
     * @param base64Data Chuỗi base64 của file MP3
     * @param filePath Đường dẫn lưu file MP3
     * @throws IOException nếu có lỗi khi lưu file
     */
    public static void saveBase64ToMp3(String base64Data, String filePath) throws IOException {
        byte[] audioBytes = Base64.getDecoder().decode(base64Data);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(audioBytes);
        }

        System.out.println("File MP3 đã được lưu tại: " + filePath);
    }
}
