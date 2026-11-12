package unishare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerDateModel;

/** Modal form for adding a new resource or editing an existing one. */
public class ResourceDialog extends JDialog {
    private final Models.User me;
    private final Models.Resource existing;
    private Models.Resource result;

    private final Ui.HintField nameF = new Ui.HintField("e.g. Engineering Mathematics - Vol. 2");
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JTextArea descA = new JTextArea(3, 20);
    private final JComboBox<String> availBox = new JComboBox<>(new String[]{"Available", "Issued"});
    private final Ui.HintField issuedToF = new Ui.HintField("User ID (blank = you)");
    private final JSpinner dateSp = new JSpinner(
            new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JLabel errLbl = Ui.label(" ", Font.BOLD, 12f, Ui.DANGER);

    public ResourceDialog(Frame owner, Models.User me, Models.Resource existing, List<String> categories) {
        super(owner, existing == null ? "Add resource" : "Edit resource", true);
        this.me = me;
        this.existing = existing;

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(26, 30, 24, 30));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel title = Ui.label(existing == null ? "Add a resource" : "Edit resource", Font.BOLD, 22f, Ui.TEXT);
        JLabel sub = Ui.label("Share something with the rest of campus.", Font.PLAIN, 13f, Ui.MUTED);
        title.setAlignmentX(0f);
        sub.setAlignmentX(0f);
        form.add(title);
        form.add(sub);
        form.add(Box.createVerticalStrut(22));

        Ui.style(nameF);
        Ui.height(nameF, 42);
        form.add(Ui.labeled("Item name", nameF));
        form.add(Box.createVerticalStrut(14));

        for (String c : categories) catBox.addItem(c);
        catBox.setEditable(true);
        Ui.style(catBox);
        Ui.height(catBox, 42);
        Ui.style(availBox);
        Ui.height(availBox, 42);
        JPanel row1 = new JPanel(new GridLayout(1, 2, 14, 0));
        row1.setOpaque(false);
        row1.add(Ui.labeled("Category (type to add new)", catBox));
        row1.add(Ui.labeled("Availability", availBox));
        row1.setAlignmentX(0f);
        form.add(row1);
        form.add(Box.createVerticalStrut(14));

        Ui.style(issuedToF);
        Ui.height(issuedToF, 42);
        dateSp.setEditor(new JSpinner.DateEditor(dateSp, "dd MMM yyyy"));
        dateSp.setFont(Ui.font(Font.PLAIN, 14f));
        Ui.height(dateSp, 42);
        JPanel row2 = new JPanel(new GridLayout(1, 2, 14, 0));
        row2.setOpaque(false);
        row2.add(Ui.labeled("Issued to", issuedToF));
        row2.add(Ui.labeled("Issued date", dateSp));
        row2.setAlignmentX(0f);
        form.add(row2);
        form.add(Box.createVerticalStrut(14));

        descA.setLineWrap(true);
        descA.setWrapStyleWord(true);
        descA.setFont(Ui.font(Font.PLAIN, 14f));
        descA.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane sp = new JScrollPane(descA);
        sp.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        sp.setPreferredSize(new Dimension(100, 84));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        form.add(Ui.labeled("Description (optional)", sp));
        form.add(Box.createVerticalStrut(10));

        errLbl.setAlignmentX(0f);
        form.add(errLbl);

        availBox.addActionListener(e -> syncIssuedFields());

        Ui.Button cancel = new Ui.Button("Cancel", Ui.Button.Kind.SECONDARY);
        Ui.Button save = new Ui.Button(existing == null ? "Add resource" : "Save changes", Ui.Button.Kind.PRIMARY);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        buttons.add(cancel);
        buttons.add(save);

        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        if (existing != null) prefill();
        syncIssuedFields();

        getRootPane().setDefaultButton(save);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        setPreferredSize(new Dimension(560, 560));
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void prefill() {
        nameF.setText(existing.name);
        catBox.setSelectedItem(existing.category);
        descA.setText(existing.description == null ? "" : existing.description);
        availBox.setSelectedItem(existing.available ? "Available" : "Issued");
        if (!existing.available) {
            issuedToF.setText(existing.issuedToId == null ? "" : existing.issuedToId);
            if (existing.issuedDate != null) {
                dateSp.setValue(java.sql.Date.valueOf(existing.issuedDate));
            }
        }
    }

    private void syncIssuedFields() {
        boolean issued = "Issued".equals(availBox.getSelectedItem());
        issuedToF.setEnabled(issued);
        dateSp.setEnabled(issued);
        ((JSpinner.DefaultEditor) dateSp.getEditor()).getTextField().setEnabled(issued);
    }

    private void onSave() {
        String name = nameF.getText().trim();
        Object catItem = catBox.getEditor().getItem();
        String cat = catItem == null ? "" : catItem.toString().trim();
        String desc = descA.getText().trim();

        if (name.isEmpty()) { error("Please enter the item name."); return; }
        if (name.length() > 120) { error("Item name is too long (max 120 characters)."); return; }
        if (cat.isEmpty()) { error("Please choose or type a category."); return; }
        if (cat.length() > 50) { error("Category name is too long (max 50 characters)."); return; }
        if (desc.length() > 500) { error("Description is too long (max 500 characters)."); return; }

        Models.Resource r = new Models.Resource();
        r.id = existing == null ? 0 : existing.id;
        r.name = name;
        r.category = cat;
        r.description = desc;
        r.ownerId = existing == null ? me.userId : existing.ownerId;
        r.available = "Available".equals(availBox.getSelectedItem());

        if (!r.available) {
            String who = issuedToF.getText().trim();
            if (who.isEmpty()) who = r.ownerId;
            try {
                if (!new UserDAO().exists(who)) {
                    error("No user with ID \"" + who + "\" exists.");
                    return;
                }
            } catch (SQLException ex) {
                error("Database error: " + ex.getMessage());
                return;
            }
            LocalDate date = new java.sql.Date(((Date) dateSp.getValue()).getTime()).toLocalDate();
            if (date.isAfter(LocalDate.now())) {
                error("Issued date cannot be in the future.");
                return;
            }
            r.issuedToId = who;
            r.issuedDate = date;
        }

        result = r;
        dispose();
    }

    private void error(String msg) {
        errLbl.setText(msg);
    }

    /** Shows the dialog and returns the filled-in resource, or null if cancelled. */
    public Models.Resource showDialog() {
        setVisible(true);
        return result;
    }
}
