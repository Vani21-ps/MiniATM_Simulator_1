package controller;

import model.DBConnection;
import model.User;

import java.sql.*;
import java.util.Scanner;

public class ATMController {
    private Scanner scanner = new Scanner(System.in);
    private Connection conn;

    public ATMController() {
        conn = DBConnection.getConnection();
    }

    public void start() {
        System.out.println("Welcome to Mini ATM Simulator");
        System.out.println("1. Login");
        System.out.println("2. Create new account");
        System.out.print("Choose option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice == 2) {
            insertUser();
            return;
        }

        System.out.print("Enter account number: ");
        String accNo = scanner.nextLine();
        System.out.print("Enter PIN: ");
        String pin = scanner.nextLine();

        User user = authenticateUser(accNo, pin);
        if (user != null) {
            showMenu(user);
        } else {
            System.out.println("Invalid credentials. Try again.");
        }
    }

    private void insertUser() {
        System.out.print("Enter new account number: ");
        String accountNumber = scanner.nextLine();

        System.out.print("Enter new PIN: ");
        String pin = scanner.nextLine();

        String hashedPin = DBConnection.hashPin(pin);

        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (account_number, pin, balance) VALUES (?, ?, ?)");
            stmt.setString(1, accountNumber);
            stmt.setString(2, hashedPin);
            stmt.setInt(3, 0);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("User account created successfully!");
            } else {
                System.out.println("Failed to create account.");
            }
        } catch (SQLException e) {
            System.out.println("Error: Account might already exist.");
            e.printStackTrace();
        }
    }

    private User authenticateUser(String accNo, String pin) {
        try {
            String hashedPin = DBConnection.hashPin(pin);

            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE account_number = ? AND pin = ?");
            stmt.setString(1, accNo);
            stmt.setString(2, hashedPin);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(accNo, rs.getString("pin"), rs.getInt("balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showMenu(User user) {
        while (true) {
            System.out.println("\n1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Change PIN");
            System.out.println("5. View Transactions");
            System.out.println("6. Exit");
            System.out.print("Choose option: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> System.out.println("Balance: " + user.getBalance());
                case 2 -> deposit(user);
                case 3 -> withdraw(user);
                case 4 -> changePIN(user);
                case 5 -> viewTransactions(user);
                case 6 -> {
                    System.out.println("Thank you!");
                    return;
                }
                default -> System.out.println("Invalid option");
            }
        }
    }

    private void deposit(User user) {
        System.out.print("Enter amount to deposit: ");
        int amount = scanner.nextInt();
        user.setBalance(user.getBalance() + amount);
        updateBalance(user);
        addTransaction(user.getAccountNumber(), "DEPOSIT", amount);
        System.out.println("Deposit successful");
    }

    private void withdraw(User user) {
        System.out.print("Enter amount to withdraw: ");
        int amount = scanner.nextInt();
        if (user.getBalance() >= amount) {
            user.setBalance(user.getBalance() - amount);
            updateBalance(user);
            addTransaction(user.getAccountNumber(), "WITHDRAW", amount);
            System.out.println("Withdrawal successful");
        } else {
            System.out.println("Insufficient balance");
        }
    }

    private void updateBalance(User user) {
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance = ? WHERE account_number = ?");
            stmt.setInt(1, user.getBalance());
            stmt.setString(2, user.getAccountNumber());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addTransaction(String accNo, String type, int amount) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO transactions(account_number, type, amount) VALUES (?, ?, ?)");
            stmt.setString(1, accNo);
            stmt.setString(2, type);
            stmt.setInt(3, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewTransactions(User user) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM transactions WHERE account_number = ? ORDER BY id DESC");
            stmt.setString(1, user.getAccountNumber());
            ResultSet rs = stmt.executeQuery();
            System.out.println("Recent Transactions:");
            while (rs.next()) {
                System.out.println(rs.getString("type") + " - ₹" + rs.getInt("amount"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void changePIN(User user) {
        scanner.nextLine();
        System.out.print("Enter current PIN: ");
        String currentPIN = scanner.nextLine();
        String hashedCurrentPIN = DBConnection.hashPin(currentPIN);
        if (!hashedCurrentPIN.equals(user.getPin())) {
            System.out.println("Incorrect current PIN.");
            return;
        }

        System.out.print("Enter new PIN: ");
        String newPIN = scanner.nextLine();
        System.out.print("Confirm new PIN: ");
        String confirmPIN = scanner.nextLine();

        if (!newPIN.equals(confirmPIN)) {
            System.out.println("PINs do not match.");
            return;
        }

        String hashedNewPIN = DBConnection.hashPin(newPIN);
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET pin = ? WHERE account_number = ?");
            stmt.setString(1, hashedNewPIN);
            stmt.setString(2, user.getAccountNumber());
            stmt.executeUpdate();
            user.setPin(hashedNewPIN);
            System.out.println("PIN changed successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
