package unishare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Searchable campus document library. Uploading is optional for readers. */
public final class DigitalLibraryFrame extends JFrame {
    private final Models.User me;
    private final DocumentDAO dao = new DocumentDAO();
    private final DocumentService service = new DocumentService();
    private final java.util.ArrayList<Document> rows = new java.util.ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Title", "Subject", "Department", "Sem", "Faculty", "Type", "Shared by", "Uploaded"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final Ui.HintField search = new Ui.HintField("Search title, subject, faculty or file name...");
    private final JComboBox<String> departmentBox = new JComboBox<>();
    private final JComboBox<String> semesterBox = new JComboBox<>(
            new String[]{"All semesters", "1", "2", "3", "4", "5", "6", "7", "8"});
    private final JTable table = new JTable(model);
    private final Ui.Button openButton = new Ui.Button("Preview / Open", Ui.Button.Kind.PRIMARY);
    private final Ui.Button downloadButton = new Ui.Button("Download", Ui.Button.Kind.SECONDARY);
    private final Ui.Button studyButton = new Ui.Button("AI Study Materials", Ui.Button.Kind.SECONDARY);
    private final JLabel countLabel = Ui.label(" ", Font.PLAIN, 12f, Ui.MUTED);

    public DigitalLibraryFrame(Models.User me) {
        super("UniShare - Digital Library");
        this.me = me;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1180, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        titles.add(Ui.label("Digital Library", Font.BOLD, 27f, Ui.TEXT));
        titles.add(Ui.label("Discover academic notes, PDFs, presentations and question papers shared by students.",
                Font.PLAIN, 13f, Ui.MUTED));
        header.add(titles, BorderLayout.WEST);
        Ui.Button upload = new Ui.Button("+ Upload Document", Ui.Button.Kind.PRIMARY);
        upload.addActionListener(e -> showUploadDialog());
        header.add(upload, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        JPanel filters = new JPanel(new BorderLayout(10, 0));
        filters.setOpaque(false);
        Ui.height(search, 42);
        Ui.style(departmentBox);
        Ui.style(semesterBox);
        departmentBox.setPreferredSize(new Dimension(190, 42));
        semesterBox.setPreferredSize(new Dimension(145, 42));
        filters.add(search, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(departmentBox);
        right.add(semesterBox);
        filters.add(right, BorderLayout.EAST);
        center.add(filters, BorderLayout.NORTH);

        table.setRowHeight(42);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(Ui.font(Font.PLAIN, 12.5f));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(Ui.PRIMARY_SOFT);
        table.setSelectionForeground(Ui.TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                    boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setBackground(new Color(0xF9FAFB));
                setForeground(Ui.MUTED);
                setFont(Ui.font(Font.BOLD, 11.5f));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable t, Object v, boolean s,
                    boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, false, r, c);
                setBackground(s ? Ui.PRIMARY_SOFT : Color.WHITE);
                setForeground(Ui.TEXT);
                setFont(Ui.font(c == 1 ? Font.BOLD : Font.PLAIN, 12f));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        int[] widths = {42, 190, 125, 115, 45, 115, 65, 125, 100};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        center.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        countLabel.setText("Loading library...");
        footer.add(countLabel, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        Ui.Button refresh = new Ui.Button("Refresh", Ui.Button.Kind.SECONDARY);
        refresh.addActionListener(e -> reload());
        openButton.setEnabled(false);
        downloadButton.setEnabled(false);
        studyButton.setEnabled(false);
        openButton.addActionListener(e -> openSelected());
        downloadButton.addActionListener(e -> downloadSelected());
        studyButton.addActionListener(e -> openStudyMaterials());
        actions.add(refresh);
        actions.add(openButton);
        actions.add(downloadButton);
        actions.add(studyButton);
        footer.add(actions, BorderLayout.EAST);
        center.add(footer, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean selected = table.getSelectedRow() >= 0;
                openButton.setEnabled(selected);
                downloadButton.setEnabled(selected);
                studyButton.setEnabled(selected);
            }
        });
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { reload(); }
            @Override public void removeUpdate(DocumentEvent e) { reload(); }
            @Override public void changedUpdate(DocumentEvent e) { reload(); }
        });
        departmentBox.addActionListener(e -> reload());
        semesterBox.addActionListener(e -> reload());
        loadDepartments();
        reload();
    }

    private void loadDepartments() {
        departmentBox.removeAllItems();
        departmentBox.addItem("All departments");
        try {
            for (String department : dao.departments()) departmentBox.addItem(department);
            if (me.department != null && !me.department.trim().isEmpty()) {
                boolean found = false;
                for (int i = 0; i < departmentBox.getItemCount(); i++) {
                    if (me.department.equals(departmentBox.getItemAt(i))) found = true;
                }
                if (!found) departmentBox.addItem(me.department);
            }
        } catch (SQLException e) {
            showError("Could not load departments", e);
        }
    }

    private void reload() {
        try {
            String department = (String) departmentBox.getSelectedItem();
            String semester = (String) semesterBox.getSelectedItem();
            Integer sem = semester == null || semester.startsWith("All") ? null : Integer.valueOf(semester);
            List<Document> found = dao.search(search.getText(), department, sem);
            rows.clear();
            rows.addAll(found);
            model.setRowCount(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            for (Document d : rows) {
                model.addRow(new Object[]{d.id, d.title, d.subject, d.department, d.semester,
                        d.faculty, d.fileType, d.uploaderName, d.uploadedAt == null ? "" : d.uploadedAt.format(fmt)});
            }
            countLabel.setText(rows.size() + (rows.size() == 1 ? " document" : " documents")
                    + " available · Uploading is optional");
        } catch (Exception e) {
            showError("Could not load the digital library", e);
        }
    }

    private Document selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int row = table.convertRowIndexToModel(viewRow);
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    private void openStudyMaterials() {
        Document d = selected();
        if (d == null) return;
        new StudyMaterialFrame(d).setVisible(true);
    }

    private void openSelected() {
        Document d = selected();
        if (d == null) return;
        try {
            Path file = service.resolve(d);
            if (!Files.isRegularFile(file)) {
                JOptionPane.showMessageDialog(this, "The stored file is missing from this computer.",
                        "File unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!Desktop.isDesktopSupported()) throw new IOException("Desktop file opening is not supported.");
            Desktop.getDesktop().open(file.toFile());
        } catch (Exception e) {
            showError("Could not open document", e);
        }
    }

    private void downloadSelected() {
        Document d = selected();
        if (d == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save document");
        chooser.setSelectedFile(new java.io.File(d.originalFileName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = chooser.getSelectedFile().toPath();
        if (Files.exists(target)) {
            int answer = JOptionPane.showConfirmDialog(this, "File already exists. Replace it?",
                    "Confirm replacement", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        try {
            Path source = service.resolve(d);
            if (!Files.isRegularFile(source)) throw new IOException("The stored file is missing.");
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this, "Document saved successfully.", "Download complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            showError("Could not download document", e);
        }
    }

    private void showUploadDialog() {
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Share a document", true);
        dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.JTextField title = new javax.swing.JTextField();
        javax.swing.JTextField subject = new javax.swing.JTextField();
        javax.swing.JTextField department = new javax.swing.JTextField(me.department);
        javax.swing.JTextField faculty = new javax.swing.JTextField();
        javax.swing.JTextArea description = new javax.swing.JTextArea(4, 24);
        JComboBox<String> semester = new JComboBox<>(new String[]{"1", "2", "3", "4", "5", "6", "7", "8"});
        Ui.style(semester);
        Ui.style(title);
        Ui.style(subject);
        Ui.style(department);
        Ui.style(faculty);
        Ui.height(title, 42);
        Ui.height(subject, 42);
        Ui.height(department, 42);
        Ui.height(faculty, 42);

        description.setFont(Ui.font(Font.PLAIN, 13f));
        description.setForeground(Ui.TEXT);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        final Path[] selectedFile = new Path[1];
        JLabel fileName = Ui.label("No file selected · PDF, PPT, DOC or TXT · Max 25 MB",
                Font.PLAIN, 12f, Ui.MUTED);
        Ui.Button choose = new Ui.Button("Browse files", Ui.Button.Kind.SECONDARY);
        choose.addActionListener(e -> {
            JFileChooser picker = new JFileChooser();
            picker.setFileFilter(new FileNameExtensionFilter(
                    "Academic files (PDF, PPT, PPTX, DOC, DOCX, TXT)",
                    "pdf", "ppt", "pptx", "doc", "docx", "txt"));
            if (picker.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = picker.getSelectedFile().toPath();
                fileName.setText(picker.getSelectedFile().getName());
                fileName.setForeground(Ui.TEXT);
            }
        });

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(heading, javax.swing.BoxLayout.Y_AXIS));
        heading.add(Ui.label("Add to your campus library", Font.BOLD, 21f, Ui.TEXT));
        JLabel subheading = Ui.label("Share useful study material with your classmates.",
                Font.PLAIN, 13f, Ui.MUTED);
        subheading.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        heading.add(subheading);
        content.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel(new java.awt.GridBagLayout());
        fields.setOpaque(false);
        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gc.insets = new java.awt.Insets(0, 0, 13, 0);
        int row = 0;

        addUploadField(fields, gc, row++, "DOCUMENT TITLE *", title);
        addUploadField(fields, gc, row++, "SUBJECT *", subject);

        JPanel departmentSemester = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        departmentSemester.setOpaque(false);
        departmentSemester.add(Ui.labeled("DEPARTMENT *", department));
        departmentSemester.add(Ui.labeled("SEMESTER *", semester));
        gc.gridy = row++;
        fields.add(departmentSemester, gc);

        addUploadField(fields, gc, row++, "FACULTY (OPTIONAL)", faculty);
        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        addUploadField(fields, gc, row++, "DESCRIPTION (OPTIONAL)", descriptionScroll);

        JPanel filePanel = new JPanel(new BorderLayout(12, 0));
        filePanel.setOpaque(false);
        filePanel.add(choose, BorderLayout.WEST);
        filePanel.add(fileName, BorderLayout.CENTER);
        addUploadField(fields, gc, row, "SELECT FILE *", filePanel);

        JPanel formCard = new Ui.Card(16, Color.WHITE);
        formCard.setLayout(new BorderLayout());
        formCard.setBorder(BorderFactory.createEmptyBorder(18, 18, 5, 18));
        formCard.add(fields, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(formCard, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);

        Ui.Button cancel = new Ui.Button("Cancel", Ui.Button.Kind.SECONDARY);
        Ui.Button upload = new Ui.Button("Share document", Ui.Button.Kind.PRIMARY);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(upload);
        content.add(actions, BorderLayout.SOUTH);

        cancel.addActionListener(e -> dialog.dispose());
        upload.addActionListener(e -> {
            if (title.getText().trim().isEmpty() || subject.getText().trim().isEmpty()
                    || department.getText().trim().isEmpty() || selectedFile[0] == null) {
                JOptionPane.showMessageDialog(dialog,
                        "Please complete the required fields and select a document.",
                        "Missing information", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Document d = new Document();
                d.title = title.getText().trim();
                d.subject = subject.getText().trim();
                d.department = department.getText().trim();
                d.semester = Integer.parseInt((String) semester.getSelectedItem());
                d.faculty = faculty.getText().trim();
                d.description = description.getText().trim();
                d.uploadedBy = me.userId;
                service.upload(d, selectedFile[0]);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Your document is now available in the Digital Library.",
                        "Upload successful", JOptionPane.INFORMATION_MESSAGE);
                loadDepartments();
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Upload failed: " + ex.getMessage(),
                        "Digital Library", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(content);
        dialog.setSize(660, 700);
        dialog.setMinimumSize(new Dimension(620, 660));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addUploadField(JPanel parent, java.awt.GridBagConstraints base, int row,
            String caption, java.awt.Component component) {
        JPanel field = new JPanel();
        field.setOpaque(false);
        field.setLayout(new javax.swing.BoxLayout(field, javax.swing.BoxLayout.Y_AXIS));
        JLabel label = Ui.label(caption, Font.BOLD, 11f, Ui.MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
        field.add(label);
        field.add(component);
        java.awt.GridBagConstraints gc = (java.awt.GridBagConstraints) base.clone();
        gc.gridy = row;
        parent.add(field, gc);
    }

    private void showError(String title, Exception e) {
        JOptionPane.showMessageDialog(this, title + ":\n" + e.getMessage(), "Digital Library",
                JOptionPane.ERROR_MESSAGE);
    }
}
