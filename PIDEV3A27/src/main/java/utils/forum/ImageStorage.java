package utils.forum;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImageStorage {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "posts");

    /**
     * Copie un fichier image dans /uploads/posts et retourne le chemin relatif enregistré en DB.
     */
    public static String copyToUploads(Path sourceFile) throws IOException {
        if (sourceFile == null) return null;

        if (!Files.exists(UPLOAD_DIR)) {
            Files.createDirectories(UPLOAD_DIR);
        }

        String original = sourceFile.getFileName().toString();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String safeName = "post_" + stamp + ext;

        Path target = UPLOAD_DIR.resolve(safeName);
        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);

        // Chemin relatif portable
        return UPLOAD_DIR.resolve(safeName).toString().replace('\\', '/');
    }

    private ImageStorage() {}
}
