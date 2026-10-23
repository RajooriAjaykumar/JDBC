package JDBC;


import java.sql.*;
import java.math.BigDecimal;
import java.util.Scanner;

public class BankingManagementSystem {

    static final String URL =
            "jdbc:mysql://localhost:3306/banking_management";
    static final String USER = "root";
    static final String PASSWORD = "Ajay@2254";

    static Scanner sc = new Scanner(System.in);

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {

        try (Connection con = getConnection()) {

            System.out.println("MySQL Connected Successfully!");

            int choice;

            do {
                System.out.println("\n===== BANKING MANAGEMENT SYSTEM =====");
                System.out.println("1. Create Account");
                System.out.println("2. Deposit Money");
                System.out.println("3. Withdraw Money");
                System.out.println("4. Fund Transfer");
                System.out.println("5. Transaction History");
                System.out.println("0. Exit");
                System.out.print("Enter your choice: ");

                choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1:
                        createAccount(con);
                        break;
                    case 2:
                        deposit(con);
                        break;
                    case 3:
                        withdraw(con);
                        break;
                    case 4:
                        transfer(con);
                        break;
                    case 5:
                        transactionHistory(con);
                        break;
                    case 0:
                        System.out.println("Thank you for using our bank!");
                        break;
                    default:
                        System.out.println("Invalid choice!");
                }

            } while (choice != 0);

        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    // 1. Account Creation
    static void createAccount(Connection con) throws SQLException {

        System.out.print("Enter customer name: ");
        String name = sc.nextLine();

        System.out.print("Enter phone: ");
        String phone = sc.nextLine();

        System.out.print("Enter email: ");
        String email = sc.nextLine();

        System.out.print("Enter opening balance: ");
        BigDecimal balance = new BigDecimal(sc.nextLine());

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("Balance cannot be negative.");
            return;
        }

        boolean oldAutoCommit = con.getAutoCommit();

        try {
            con.setAutoCommit(false);

            String customerSql =
                    "INSERT INTO customers(customer_name, phone, email) "
                    + "VALUES (?, ?, ?)";

            int customerId;

            try (PreparedStatement ps = con.prepareStatement(
                    customerSql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, name);
                ps.setString(2, phone);
                ps.setString(3, email);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Customer ID was not generated.");
                    }
                    customerId = rs.getInt(1);
                }
            }

            String accountSql =
                    "INSERT INTO accounts(customer_id, balance) VALUES (?, ?)";

            int accountId;

            try (PreparedStatement ps = con.prepareStatement(
                    accountSql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, customerId);
                ps.setBigDecimal(2, balance);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Account ID was not generated.");
                    }
                    accountId = rs.getInt(1);
                }
            }

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                addHistory(con, accountId, "DEPOSIT", balance,
                        "Opening balance");
            }

            con.commit();

            System.out.println("Account created successfully!");
            System.out.println("Customer ID: " + customerId);
            System.out.println("Account ID: " + accountId);
            System.out.println("Balance: " + balance);

        } catch (SQLException e) {
            rollback(con, e);
            System.out.println("Account creation failed: " + e.getMessage());
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    // 2. Deposit
    static void deposit(Connection con) throws SQLException {

        System.out.print("Enter account ID: ");
        int accountId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter deposit amount: ");
        BigDecimal amount = new BigDecimal(sc.nextLine());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        boolean oldAutoCommit = con.getAutoCommit();

        try {
            con.setAutoCommit(false);

            String sql =
                    "UPDATE accounts SET balance = balance + ? "
                    + "WHERE account_id = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setBigDecimal(1, amount);
                ps.setInt(2, accountId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Account not found.");
                }
            }

            addHistory(con, accountId, "DEPOSIT", amount, "Cash deposit");

            con.commit();
            System.out.println("Deposit successful!");

        } catch (SQLException e) {
            rollback(con, e);
            System.out.println("Deposit failed: " + e.getMessage());
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    // 3. Withdrawal
    static void withdraw(Connection con) throws SQLException {

        System.out.print("Enter account ID: ");
        int accountId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter withdrawal amount: ");
        BigDecimal amount = new BigDecimal(sc.nextLine());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        boolean oldAutoCommit = con.getAutoCommit();

        try {
            con.setAutoCommit(false);

            // Deduct only if sufficient balance is available.
            String sql =
                    "UPDATE accounts SET balance = balance - ? "
                    + "WHERE account_id = ? AND balance >= ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setBigDecimal(1, amount);
                ps.setInt(2, accountId);
                ps.setBigDecimal(3, amount);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException(
                            "Account not found or insufficient balance.");
                }
            }

            addHistory(con, accountId, "WITHDRAW", amount,
                    "Cash withdrawal");

            con.commit();
            System.out.println("Withdrawal successful!");

        } catch (SQLException e) {
            rollback(con, e);
            System.out.println("Withdrawal failed: " + e.getMessage());
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    // 4. Fund Transfer - TRANSACTION / COMMIT / ROLLBACK
    static void transfer(Connection con) throws SQLException {

        System.out.print("Enter sender account ID: ");
        int senderId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter receiver account ID: ");
        int receiverId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter transfer amount: ");
        BigDecimal amount = new BigDecimal(sc.nextLine());

        if (senderId == receiverId) {
            System.out.println("Sender and receiver must be different.");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        boolean oldAutoCommit = con.getAutoCommit();

        try {
            con.setAutoCommit(false);

            // Lock both accounts in a consistent order.
            int firstId = Math.min(senderId, receiverId);
            int secondId = Math.max(senderId, receiverId);

            String lockSql =
                    "SELECT account_id FROM accounts "
                    + "WHERE account_id IN (?, ?) "
                    + "ORDER BY account_id FOR UPDATE";

            int found = 0;

            try (PreparedStatement ps = con.prepareStatement(lockSql)) {
                ps.setInt(1, firstId);
                ps.setInt(2, secondId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        found++;
                    }
                }
            }

            if (found != 2) {
                throw new SQLException(
                        "Sender or receiver account does not exist.");
            }

            // Deduct money only if the sender has sufficient balance.
            String debitSql =
                    "UPDATE accounts SET balance = balance - ? "
                    + "WHERE account_id = ? AND balance >= ?";

            try (PreparedStatement ps = con.prepareStatement(debitSql)) {
                ps.setBigDecimal(1, amount);
                ps.setInt(2, senderId);
                ps.setBigDecimal(3, amount);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Insufficient balance.");
                }
            }

            // Credit receiver.
            String creditSql =
                    "UPDATE accounts SET balance = balance + ? "
                    + "WHERE account_id = ?";

            try (PreparedStatement ps = con.prepareStatement(creditSql)) {
                ps.setBigDecimal(1, amount);
                ps.setInt(2, receiverId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Receiver credit failed.");
                }
            }

            // Record both sides of the transfer.
            addHistory(con, senderId, "TRANSFER_OUT", amount,
                    "Transfer to account " + receiverId);

            addHistory(con, receiverId, "TRANSFER_IN", amount,
                    "Transfer from account " + senderId);

            // Save all changes together.
            con.commit();

            System.out.println("Fund transfer successful!");
            System.out.println("Amount transferred: " + amount);

        } catch (SQLException e) {
            rollback(con, e);
            System.out.println(
                    "Transfer failed. All changes rolled back: "
                    + e.getMessage());
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    // Common method to record transaction history
    static void addHistory(Connection con, int accountId,
                           String type, BigDecimal amount,
                           String description) throws SQLException {

        String sql =
                "INSERT INTO transactions "
                + "(account_id, transaction_type, amount, description) "
                + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setString(4, description);
            ps.executeUpdate();
        }
    }

    // Roll back database changes if any operation fails
    static void rollback(Connection con, SQLException originalError) {
        try {
            con.rollback();
        } catch (SQLException rollbackError) {
            originalError.addSuppressed(rollbackError);
            System.out.println("Rollback error: "
                    + rollbackError.getMessage());
        }
    }

    // 5. Transaction History
    static void transactionHistory(Connection con) throws SQLException {

        System.out.print("Enter account ID: ");
        int accountId = Integer.parseInt(sc.nextLine());

        String sql =
                "SELECT transaction_id, transaction_type, amount, "
                + "transaction_date, description "
                + "FROM transactions WHERE account_id = ? "
                + "ORDER BY transaction_date DESC, transaction_id DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n===== TRANSACTION HISTORY =====");

                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println(
                            "Transaction ID: "
                            + rs.getInt("transaction_id"));
                    System.out.println(
                            "Type: " + rs.getString("transaction_type"));
                    System.out.println(
                            "Amount: " + rs.getBigDecimal("amount"));
                    System.out.println(
                            "Date: " + rs.getTimestamp("transaction_date"));
                    System.out.println(
                            "Description: " + rs.getString("description"));
                    System.out.println("------------------------------");
                }

                if (!found) {
                    System.out.println("No transaction history found.");
                }
            }
        }
    }
}

