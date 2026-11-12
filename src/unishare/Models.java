package unishare;

import java.time.LocalDate;

/** Plain data holders shared by the UI and the DAO layer. */
public final class Models {
    private Models() {}

    public static final class User {
        public final String userId;
        public final String fullName;
        public final String department;

        public User(String userId, String fullName, String department) {
            this.userId = userId;
            this.fullName = fullName;
            this.department = department == null ? "" : department;
        }

        public String initials() {
            String[] parts = fullName.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length && sb.length() < 2; i++) {
                if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
            return sb.length() == 0 ? userId.substring(0, 1).toUpperCase() : sb.toString();
        }
    }

    public static final class Resource {
        public int id;
        public String name;
        public String category;
        public String description;
        public String ownerId;
        public String ownerName;
        public boolean available = true;
        public String issuedToId;
        public String issuedToName;
        public LocalDate issuedDate;
        public LocalDate dueDate;
    }

    public static final class BorrowRecord {
        public long id;
        public String resourceName;
        public String ownerName;
        public String borrowerName;
        public String ownerId;
        public String borrowerId;
        public LocalDate borrowedOn;
        public LocalDate dueDate;
        public LocalDate returnedOn;

        public String status() {
            if (returnedOn != null) return "Returned";
            return LocalDate.now().isAfter(dueDate) ? "Overdue" : "Borrowed";
        }
    }

}
