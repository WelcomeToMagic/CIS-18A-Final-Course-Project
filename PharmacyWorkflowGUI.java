/*
    Hector Mejia
    CIS-18A Course Project
    Pharmacy Workflow Helper - w GUI (Swing)
*/

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PharmacyWorkflowGUI {

    // Backend (reuses your Phase 1 classes)
    private final OrderManager manager = new OrderManager();
    private final ScriptGenerator scripts = new ScriptGenerator();

    // "Time" simulation
    private int currentDay = 0;

    // GUI Components
    private JFrame frame;
    private JLabel dayLabel;
    private JTextField orderIdField;
    private JComboBox<Priority> priorityBox;
    private JTextField notesField;
    private JTextArea outputArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PharmacyWorkflowGUI().buildAndShow());
    }

    private void buildAndShow() {
        frame = new JFrame("Pharmacy Workflow Helper (GUI)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 600);

        // Top panel: inputs 
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        dayLabel = new JLabel("Current Day: " + currentDay);

        orderIdField = new JTextField(12);
        priorityBox = new JComboBox<>(Priority.values());
        notesField = new JTextField(30);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        top.add(dayLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        top.add(new JLabel("Order ID (non-PHI):"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        top.add(orderIdField, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        top.add(new JLabel("Priority:"), gbc);

        gbc.gridx = 3; gbc.gridy = 1;
        top.add(priorityBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        top.add(new JLabel("Notes (optional):"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 3;
        top.add(notesField, gbc);

        // Buttons panel 
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));

        JButton addOrUpdateBtn = new JButton("Add / Update");
        JButton updateBtn = new JButton("Update (explicit)");
        JButton workQueueBtn = new JButton("View Work Queue");
        JButton viewAllBtn = new JButton("View All");
        JButton dueTodayBtn = new JButton("View Due Today");
        JButton overdueBtn = new JButton("View Overdue");
        JButton scriptsBtn = new JButton("Generate Scripts");
        JButton advanceDayBtn = new JButton("Advance Day (+1)");
        JButton saveReportBtn = new JButton("Save Report");
        JButton clearBtn = new JButton("Clear Output");

        buttons.add(addOrUpdateBtn);
        buttons.add(updateBtn);
        buttons.add(workQueueBtn);
        buttons.add(viewAllBtn);
        buttons.add(dueTodayBtn);
        buttons.add(overdueBtn);
        buttons.add(scriptsBtn);
        buttons.add(advanceDayBtn);
        buttons.add(saveReportBtn);
        buttons.add(clearBtn);

        // Output area 
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(outputArea);

        // Layout
        JPanel north = new JPanel(new BorderLayout());
        north.add(top, BorderLayout.NORTH);
        north.add(buttons, BorderLayout.SOUTH);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(north, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);

        // Button actions 
        addOrUpdateBtn.addActionListener(e -> handleAddOrUpdate());
        updateBtn.addActionListener(e -> handleExplicitUpdate());
        workQueueBtn.addActionListener(e -> showWorkQueue());
        viewAllBtn.addActionListener(e -> showAllSorted());
        dueTodayBtn.addActionListener(e -> showDueToday());
        overdueBtn.addActionListener(e -> showOverdue());
        scriptsBtn.addActionListener(e -> showScriptsForId());
        advanceDayBtn.addActionListener(e -> advanceDay());
        saveReportBtn.addActionListener(e -> saveReport());
        clearBtn.addActionListener(e -> outputArea.setText(""));

        // Show
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Initial message
        appendLine("GUI ready. Add an order to begin.");
        appendLine("Rules: STAT due today & first; URGENT due today (overnight); ROUTINE due in 2 days.");
    }

    // Actions
    private void handleAddOrUpdate() {
        String id = orderIdField.getText().trim();
        if (id.isEmpty()) {
            showError("Order ID cannot be empty.");
            return;
        }

        Priority p = (Priority) priorityBox.getSelectedItem();
        String notes = notesField.getText();

        boolean wasUpdated = manager.addOrUpdate(id, p, notes, currentDay);

        if (wasUpdated) {
            appendLine("UPDATED existing order: " + id);
        } else {
            appendLine("ADDED new order: " + id);
        }

        Order o = manager.findOrder(id);
        appendLine(o.toDisplayString(currentDay));
        appendLine("");

        // small UX: keep ID but clear notes
        notesField.setText("");
    }

    private void handleExplicitUpdate() {
        String id = orderIdField.getText().trim();
        if (id.isEmpty()) {
            showError("Enter the Order ID you want to update.");
            return;
        }

        Order existing = manager.findOrder(id);
        if (existing == null) {
            showError("Order not found. Use Add/Update to add it first.");
            return;
        }

        Priority p = (Priority) priorityBox.getSelectedItem();
        String notes = notesField.getText();

        // explicit update uses the Order.update method directly when orders status changes
        existing.update(p, notes, currentDay);

        appendLine("EXPLICIT UPDATE applied to: " + id);
        appendLine(existing.toDisplayString(currentDay));
        appendLine("");

        notesField.setText("");
    }

    private void showAllSorted() {
        List<Order> list = manager.getAllSorted();
        appendLine("--- ALL ORDERS (sorted by due day, then priority) ---");
        if (list.isEmpty()) {
            appendLine("No orders.");
        } else {
            for (Order o : list) appendLine(o.toDisplayString(currentDay));
        }
        appendLine("");
    }

    private void showDueToday() {
        List<Order> list = manager.dueToday(currentDay);
        appendLine("--- DUE TODAY ---");
        if (list.isEmpty()) {
            appendLine("None.");
        } else {
            for (Order o : list) appendLine(o.toDisplayString(currentDay));
        }
        appendLine("");
    }

    private void showOverdue() {
        List<Order> list = manager.overdue(currentDay);
        appendLine("--- OVERDUE ---");
        if (list.isEmpty()) {
            appendLine("None.");
        } else {
            for (Order o : list) appendLine(o.toDisplayString(currentDay));
        }
        appendLine("");
    }

    private void showWorkQueue() {
        List<Order> all = manager.getAllSorted();
        appendLine("--- WORK QUEUE ---");
        if (all.isEmpty()) {
            appendLine("No orders.");
            appendLine("");
            return;
        }

        List<Order> dueToday = new ArrayList<>();
        List<Order> onTrack = new ArrayList<>();
        List<Order> overdue = new ArrayList<>();

        for (Order o : all) {
            String status = o.getStatus(currentDay);
            switch (status) {
                case "DUE_TODAY" -> dueToday.add(o);
                case "OVERDUE" -> overdue.add(o);
                default -> onTrack.add(o);
            }
        }

        // Ensure STAT then URGENT for dueToday
        dueToday.sort(Comparator.comparingInt(o -> o.getPriority().rank()));

        appendLine("[DUE TODAY]");
        if (dueToday.isEmpty()) appendLine("None.");
        else for (Order o : dueToday) appendLine(o.toDisplayString(currentDay));

        appendLine("");
        appendLine("[ON TRACK]");
        if (onTrack.isEmpty()) appendLine("None.");
        else for (Order o : onTrack) appendLine(o.toDisplayString(currentDay));

        appendLine("");
        appendLine("[OVERDUE]");
        if (overdue.isEmpty()) appendLine("None.");
        else for (Order o : overdue) appendLine(o.toDisplayString(currentDay));

        appendLine("");
    }

    private void showScriptsForId() {
        String id = orderIdField.getText().trim();
        if (id.isEmpty()) {
            showError("Enter an Order ID to generate scripts.");
            return;
        }

        Order o = manager.findOrder(id);
        if (o == null) {
            showError("Order not found.");
            return;
        }

        appendLine("--- SCRIPTS for " + o.getOrderId() + " ---");
        appendLine(scripts.handoffScript(o));
        appendLine(scripts.compoundingScript(o));
        appendLine(scripts.deliveryScript(o));
        appendLine("");
    }

    private void advanceDay() {
        currentDay++;
        dayLabel.setText("Current Day: " + currentDay);
        appendLine("Day advanced to Day " + currentDay + ".");
        appendLine("");
    }

    private void saveReport() {
        String filename = "report_day" + currentDay + ".txt";

        List<Order> all = manager.getAllSorted();
        List<Order> dueToday = manager.dueToday(currentDay);
        List<Order> overdue = manager.overdue(currentDay);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Pharmacy Workflow Helper Report ===\n");
        sb.append("Day: ").append(currentDay).append("\n\n");
        sb.append("Totals:\n");
        sb.append("  All orders: ").append(all.size()).append("\n");
        sb.append("  Due today:  ").append(dueToday.size()).append("\n");
        sb.append("  Overdue:    ").append(overdue.size()).append("\n\n");
        sb.append("--- Work Queue (sorted) ---\n");

        if (all.isEmpty()) sb.append("No orders.\n");
        else for (Order o : all) sb.append(o.toDisplayString(currentDay)).append("\n");

        try (PrintWriter out = new PrintWriter(filename)) {
            out.print(sb.toString());
            appendLine("Report saved to: " + filename);
            appendLine("");
        } catch (Exception ex) {
            showError("Error saving report: " + ex.getMessage());
        }
    }

    // Helpers
    private void appendLine(String s) {
        outputArea.append(s + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
