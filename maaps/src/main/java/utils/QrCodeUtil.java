package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * QR image generation using ZXing.
 */
public final class QrCodeUtil {

    private QrCodeUtil() {}

    public static Path generatePng(String text, int sizePx, Path outputFile) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Texte QR vide.");
        if (sizePx <= 0) sizePx = 260;
        try {
            if (outputFile.getParent() != null) Files.createDirectories(outputFile.getParent());

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            MatrixToImageWriter.writeToPath(matrix, "PNG", outputFile);
            return outputFile;
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération QR: " + e.getMessage(), e);
        }
    }
}
