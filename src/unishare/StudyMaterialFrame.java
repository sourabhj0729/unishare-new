package unishare;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/** Generates source-grounded study material through the local Python AI service. */
public final class StudyMaterialFrame extends JFrame {
    private static final String[] TYPES = {
        "MCQs with answers",
        "Flashcards",
        "Important questions",
        "Short and long-answer questions",
        "Chapter-wise revision notes"
    };

    private final Document document;
    private final JComboBox<String> typeBox = new JComboBox<>(TYPES);
    private final JTextArea output = new JTextArea();
    private final Ui.Button generate = new Ui.Button("Generate study material", Ui.Button.Kind.PRIMARY);
    private final JLabel status = Ui.label("Choose a format and generate from this document.",
            Font.PLAIN, 12f, Ui.MUTED);

    public StudyMaterialFrame(Document document) {
        super("UniShare - AI Study Materials");
        this.document = document;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 680);
        setMinimumSize(new Dimension(700, 520));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        titles.add(Ui.label("AI Study Materials", Font.BOLD, 24f, Ui.TEXT));
        titles.add(Ui.label("Generated from: " + document.title, Font.PLAIN, 13f, Ui.MUTED));
        header.add(titles, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel controls = new JPanel(new BorderLayout(10, 0));
        controls.setOpaque(false);
        Ui.style(typeBox);
        typeBox.setPreferredSize(new Dimension(280, 42));
        controls.add(Ui.labeled("STUDY MATERIAL TYPE", typeBox), BorderLayout.CENTER);
        generate.addActionListener(e -> generateMaterial());
        controls.add(generate, BorderLayout.EAST);

        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setFont(Ui.font(Font.PLAIN, 14f));
        output.setForeground(Ui.TEXT);
        output.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(controls, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        center.add(status, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void generateMaterial() {
        String type = (String) typeBox.getSelectedItem();
        generate.setEnabled(false);
        status.setText("Extracting document text and generating material...");
        output.setText("");

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                String body = "{\"file_path\":\"" + jsonEscape(document.filePath)
                        + "\",\"material_type\":\"" + jsonEscape(type) + "\"}";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:8000/generate"))
                        .timeout(Duration.ofMinutes(3))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient().send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException(readJsonString(response.body(), "detail",
                            "AI service returned HTTP " + response.statusCode()));
                }
                return readJsonString(response.body(), "content", "The service returned an empty response.");
            }

            @Override protected void done() {
                generate.setEnabled(true);
                try {
                    output.setText(get());
                    output.setCaretPosition(0);
                    status.setText("Generated from the selected document. Verify important answers against the original.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    status.setText("Generation could not be completed.");
                    JOptionPane.showMessageDialog(StudyMaterialFrame.this,
                            cause.getMessage() + "\n\nCheck that the AI service is running and OPENAI_API_KEY is set.",
                            "AI Study Materials", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String readJsonString(String json, String key, String fallback) {
        String marker = "\"" + key + "\"";
        int keyAt = json.indexOf(marker);
        if (keyAt < 0) return fallback;
        int colon = json.indexOf(':', keyAt + marker.length());
        int start = colon < 0 ? -1 : json.indexOf('"', colon + 1);
        if (start < 0) return fallback;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                switch (ch) {
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    default: value.append(ch);
                }
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                return value.toString();
            } else {
                value.append(ch);
            }
        }
        return fallback;
    }
}
