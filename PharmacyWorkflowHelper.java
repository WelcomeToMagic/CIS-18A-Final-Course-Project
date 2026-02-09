/*
    Hector Mejia
    CIS-18A Final Project
    Pharmacy Workflow Helper
*/

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;       // Import libraries

// Establish priority cases
enum Priority {
    STAT, URGENT, ROUTINE;

    // Convert user input to Priority safely
    static Priority fromString(String s) {
        return Priority.valueOf(s.trim().toUpperCase());
    }

    // Smaller number = higher priority
    int rank() {
        return switch (this) {
            case STAT -> 1; // Same day deliveries due first 
            case URGENT -> 2; // End of day deliveries or overnights
            case ROUTINE -> 3; // Due 2 days or later
        };
    }
}

class Order {
    private final String orderId;     // non-PHI identifier to follow HIPAA guidelines
    private Priority priority;
    private int receivedDay;
    private int dueDay;
    private String notes;

    Order(String orderId, Priority priority, int receivedDay, String notes) {
        this.orderId = orderId;
        this.priority = priority;
        this.receivedDay = receivedDay;
        this.dueDay = computeDueDay(priority, receivedDay);
        this.notes = (notes == null) ? "" : notes.trim();
    }

    private int computeDueDay(Priority p, int receivedDay) {
        // Your current rules:
        // STAT -> same day
        // URGENT -> same day (overnight)
        // ROUTINE -> +2 days
        return switch (p) {
            case STAT, URGENT -> receivedDay;
            case ROUTINE -> receivedDay + 2;
        };
    }

// --- getters ---
    String getOrderId() { return orderId; }
    Priority getPriority() { return priority; }
    int getReceivedDay() { return receivedDay; }
    int getDueDay() { return dueDay; }
    String getNotes() { return notes; }

    // --- update behavior (encapsulation) ---
    void update(Priority newPriority, String newNotes, int currentDay) {
        // Treat update as "this was re-triaged today"
        this.receivedDay = currentDay;
        this.priority = newPriority;
        this.dueDay = computeDueDay(newPriority, currentDay);

        if (newNotes != null) {
            String trimmed = newNotes.trim();
            // If user leaves notes blank during update, keep previous notes
            if (!trimmed.isEmpty()) this.notes = trimmed;
        }
    }

    String getStatus(int currentDay) {
        if (currentDay > dueDay) return "OVERDUE";
        if (currentDay == dueDay) return "DUE_TODAY";
        return "ON_TRACK";
    }

    String toDisplayString(int currentDay) {
        return String.format(
                "%s | %s | received Day %d | due Day %d | %s | notes: %s",
                orderId, priority, receivedDay, dueDay, getStatus(currentDay),
                notes.isEmpty() ? "-" : notes
        );
    }
}

class OrderManager {
    private final List<Order> orders = new ArrayList<>();

    Order findOrder(String orderId) {
        String key = orderId.trim();
        for (Order o : orders) {
            if (o.getOrderId().equalsIgnoreCase(key)) return o;
        }
        return null;
    }

    // Exception: Add if new, otherwise update existing record
    boolean addOrUpdate(String orderId, Priority priority, String notes, int currentDay) {
        Order existing = findOrder(orderId);
        if (existing == null) {
            orders.add(new Order(orderId, priority, currentDay, notes));
            return false; // false = it was added (not updated)
        } else {
            existing.update(priority, notes, currentDay);
            return true;  // true = it was updated
        }
    }

    List<Order> getAllSorted() {
        List<Order> copy = new ArrayList<>(orders);
        copy.sort(Comparator
                .comparingInt(Order::getDueDay)
                .thenComparingInt(o -> o.getPriority().rank())
                .thenComparing(o -> o.getOrderId().toUpperCase())
        );
        return copy;
    }

    List<Order> dueToday(int currentDay) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (o.getDueDay() == currentDay) result.add(o);
        }
        result.sort(Comparator
                .comparingInt(Order::getDueDay)
                .thenComparingInt(o -> o.getPriority().rank())
        );
        return result;
    }

    List<Order> overdue(int currentDay) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (currentDay > o.getDueDay()) result.add(o);
        }
        result.sort(Comparator
                .comparingInt(Order::getDueDay)
                .thenComparingInt(o -> o.getPriority().rank())
        );
        return result;
    }
}

class ScriptGenerator {

    String handoffScript(Order o) {
        return switch (o.getPriority()) {
            case STAT -> "STAT — DO FIRST: " + o.getOrderId() + " | Due: TODAY"
                    + (o.getNotes().isEmpty() ? "" : " | Notes: " + o.getNotes());
            case URGENT -> "URGENT — EOD SHIP: " + o.getOrderId() + " | Due: TODAY (overnight)"
                    + (o.getNotes().isEmpty() ? "" : " | Notes: " + o.getNotes());
            case ROUTINE -> "ROUTINE: " + o.getOrderId() + " | Due: Day " + o.getDueDay()
                    + (o.getNotes().isEmpty() ? "" : " | Notes: " + o.getNotes());
        };
    }

    String compoundingScript(Order o) {
        return "COMPOUND: " + o.getOrderId()
                + " | Priority: " + o.getPriority()
                + " | Due Day: " + o.getDueDay()
                + (o.getNotes().isEmpty() ? "" : " | Notes: " + o.getNotes());
    }

    String deliveryScript(Order o) {
        String shipType = switch (o.getPriority()) {
            case STAT -> "Same-day";
            case URGENT -> "Overnight";
            case ROUTINE -> "Standard";
        };

        return "DELIVERY: " + o.getOrderId()
                + " | Due Day: " + o.getDueDay()
                + " | Type: " + shipType
                + (o.getNotes().isEmpty() ? "" : " | Notes: " + o.getNotes());
    }
}

public class PharmacyWorkflowHelper {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        OrderManager manager = new OrderManager();
        ScriptGenerator scripts = new ScriptGenerator();

        int currentDay = 0;
        boolean running = true;

        while (running) {
            System.out.println("\n=== Pharmacy Workflow Helper ===");
            System.out.println("Current Day: " + currentDay);
            System.out.println("1) Add order (or update if ID exists)");
            System.out.println("2) View all orders (sorted)");
            System.out.println("3) View DUE TODAY");
            System.out.println("4) View OVERDUE");
            System.out.println("5) Generate scripts for an order");
            System.out.println("6) Advance day (+1)");
            System.out.println("7) Exit");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> addOrUpdateFlow(sc, manager, currentDay);
                case "2" -> viewAllFlow(manager, currentDay);
                case "3" -> viewDueTodayFlow(manager, currentDay);
                case "4" -> viewOverdueFlow(manager, currentDay);
                case "5" -> scriptsFlow(sc, manager, scripts);
                case "6" -> {
                    currentDay++;
                    System.out.println("Day advanced. Current Day = " + currentDay);
                }
                case "7" -> {
                    running = false;
                    System.out.println("Goodbye.");
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }

        sc.close();
    }

    private static void addOrUpdateFlow(Scanner sc, OrderManager manager, int currentDay) {
        System.out.print("Enter order ID (non-PHI): ");
        String orderId = sc.nextLine().trim();
        if (orderId.isEmpty()) {
            System.out.println("Order ID cannot be empty.");
            return;
        }

        Priority priority;
        try {
            System.out.print("Enter priority (STAT / URGENT / ROUTINE): ");
            priority = Priority.fromString(sc.nextLine());
        } catch (Exception e) {
            System.out.println("Invalid priority. Must be STAT, URGENT, or ROUTINE.");
            return;
        }

        System.out.print("Notes (optional): ");
        String notes = sc.nextLine();

        boolean wasUpdated = manager.addOrUpdate(orderId, priority, notes, currentDay);
        if (wasUpdated) {
            System.out.println("Order existed — UPDATED (priority/notes/due day refreshed).");
        } else {
            System.out.println("New order ADDED.");
        }

        Order o = manager.findOrder(orderId);
        System.out.println(o.toDisplayString(currentDay));
    }

    private static void viewAllFlow(OrderManager manager, int currentDay) {
        List<Order> list = manager.getAllSorted();
        if (list.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }
        System.out.println("\n--- ALL ORDERS (sorted by due day, then priority) ---");
        for (Order o : list) {
            System.out.println(o.toDisplayString(currentDay));
        }
    }

    private static void viewDueTodayFlow(OrderManager manager, int currentDay) {
        List<Order> list = manager.dueToday(currentDay);
        if (list.isEmpty()) {
            System.out.println("No orders due today.");
            return;
        }
        System.out.println("\n--- DUE TODAY ---");
        for (Order o : list) {
            System.out.println(o.toDisplayString(currentDay));
        }
    }

    private static void viewOverdueFlow(OrderManager manager, int currentDay) {
        List<Order> list = manager.overdue(currentDay);
        if (list.isEmpty()) {
            System.out.println("No overdue orders.");
            return;
        }
        System.out.println("\n--- OVERDUE ---");
        for (Order o : list) {
            System.out.println(o.toDisplayString(currentDay));
        }
    }

    private static void scriptsFlow(Scanner sc, OrderManager manager, ScriptGenerator scripts) {
        System.out.print("Enter order ID to generate scripts: ");
        String id = sc.nextLine().trim();
        Order o = manager.findOrder(id);

        if (o == null) {
            System.out.println("Order not found.");
            return;
        }

        System.out.println("\n--- SCRIPTS ---");
        System.out.println(scripts.handoffScript(o));
        System.out.println(scripts.compoundingScript(o));
        System.out.println(scripts.deliveryScript(o));
    }
} // End Program