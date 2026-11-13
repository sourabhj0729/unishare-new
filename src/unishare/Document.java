package unishare;

import java.time.LocalDateTime;

/** Metadata for a file shared in the campus digital library. */
public final class Document {
    public int id;
    public String title;
    public String description;
    public String originalFileName;
    public String storedFileName;
    public String filePath;
    public String fileType;
    public String subject;
    public String department;
    public int semester;
    public String faculty;
    public String uploadedBy;
    public String uploaderName;
    public LocalDateTime uploadedAt;
}
