package unishare;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/** Validates and stores uploaded library files before saving their metadata. */
public final class DocumentService {
    public static final long MAX_FILE_BYTES = 25L * 1024L * 1024L;
    private static final Path STORAGE_DIR = Paths.get("uploads", "documents");
    private static final String[] ALLOWED = {".pdf", ".ppt", ".pptx", ".doc", ".docx", ".txt"};

    private final DocumentDAO dao = new DocumentDAO();

    public void upload(Document document, Path sourceFile) throws Exception {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IllegalArgumentException("Please select a valid file.");
        }
        long size = Files.size(sourceFile);
        if (size == 0) throw new IllegalArgumentException("The selected file is empty.");
        if (size > MAX_FILE_BYTES) throw new IllegalArgumentException("Maximum upload size is 25 MB.");

        String original = sourceFile.getFileName().toString();
        String lower = original.toLowerCase(Locale.ROOT);
        String extension = extensionOf(lower);
        boolean allowed = false;
        for (String ext : ALLOWED) if (ext.equals(extension)) allowed = true;
        if (!allowed) throw new IllegalArgumentException("Supported formats: PDF, PPT, PPTX, DOC, DOCX and TXT.");

        if (".pdf".equals(extension)) {
            byte[] signature = new byte[5];
            try (java.io.InputStream in = Files.newInputStream(sourceFile)) {
                if (in.read(signature) != 5 || !"%PDF-".equals(new String(signature, java.nio.charset.StandardCharsets.US_ASCII))) {
                    throw new IllegalArgumentException("The selected file does not appear to be a valid PDF.");
                }
            }
        }

        Files.createDirectories(STORAGE_DIR);
        String storedName = UUID.randomUUID().toString() + extension;
        Path destination = STORAGE_DIR.resolve(storedName).normalize();
        if (!destination.startsWith(STORAGE_DIR.normalize())) {
            throw new IOException("Invalid destination path.");
        }
        Files.copy(sourceFile, destination, StandardCopyOption.COPY_ATTRIBUTES);

        document.originalFileName = original;
        document.storedFileName = storedName;
        document.filePath = Paths.get("uploads", "documents", storedName).toString().replace('\\', '/');
        document.fileType = extension.substring(1).toUpperCase(Locale.ROOT);
        try {
            dao.add(document);
        } catch (Exception e) {
            Files.deleteIfExists(destination);
            throw e;
        }
    }

    public Path resolve(Document document) {
        Path path = Paths.get(document.filePath).normalize();
        if (!path.startsWith(STORAGE_DIR.normalize())) {
            throw new IllegalArgumentException("The stored document path is invalid.");
        }
        return path;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }
}
