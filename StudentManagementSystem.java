package JDBC;


import java.sql.*;


import java.sql.Date;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

public class StudentManagementSystem {

    static final String URL =
            "jdbc:mysql://localhost:3306/student_management";
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
                System.out.println("\n===== STUDENT MANAGEMENT SYSTEM =====");
                System.out.println("1. Register Student");
                System.out.println("2. View Students");
                System.out.println("3. Update Student");
                System.out.println("4. Delete Student");
                System.out.println("5. View Courses");
                System.out.println("6. Enroll Student");
                System.out.println("7. Mark Attendance");
                System.out.println("8. Enter Marks");
                System.out.println("9. Generate Result");
                System.out.println("10. View Student Course Details");
                System.out.println("0. Exit");
                System.out.print("Enter your choice: ");

                choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1:
                        registerStudent(con);
                        break;
                    case 2:
                        viewStudents(con);
                        break;
                    case 3:
                        updateStudent(con);
                        break;
                    case 4:
                        deleteStudent(con);
                        break;
                    case 5:
                        viewCourses(con);
                        break;
                    case 6:
                        enrollStudent(con);
                        break;
                    case 7:
                        markAttendance(con);
                        break;
                    case 8:
                        enterMarks(con);
                        break;
                    case 9:
                        generateResult(con);
                        break;
                    case 10:
                        viewStudentCourseDetails(con);
                        break;
                    case 0:
                        System.out.println("Thank you!");
                        break;
                    default:
                        System.out.println("Invalid choice!");
                }

            } while (choice != 0);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    // 1. Student Registration - CREATE
    static void registerStudent(Connection con) throws SQLException {
        System.out.print("Enter student name: ");
        String name = sc.nextLine();

        System.out.print("Enter email: ");
        String email = sc.nextLine();

        System.out.print("Enter phone: ");
        String phone = sc.nextLine();

        String sql = "INSERT INTO students " +
                "(student_name, email, phone) VALUES (?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);

            ps.executeUpdate();
            System.out.println("Student registered successfully!");
        }
    }

    // 2. View Students - READ
    static void viewStudents(Connection con) throws SQLException {
        String sql = "SELECT * FROM students";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nID\tName\t\tEmail\t\tPhone");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("student_id") + "\t" +
                        rs.getString("student_name") + "\t" +
                        rs.getString("email") + "\t" +
                        rs.getString("phone"));
            }
        }
    }

    // 3. Update Student - UPDATE
    static void updateStudent(Connection con) throws SQLException {
        System.out.print("Enter student ID: ");
        int id = Integer.parseInt(sc.nextLine());

        System.out.print("Enter new name: ");
        String name = sc.nextLine();

        System.out.print("Enter new email: ");
        String email = sc.nextLine();

        System.out.print("Enter new phone: ");
        String phone = sc.nextLine();

        String sql = "UPDATE students SET student_name=?, " +
                "email=?, phone=? WHERE student_id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setInt(4, id);

            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Student updated successfully!");
            else
                System.out.println("Student not found.");
        }
    }

    // 4. Delete Student - DELETE
    static void deleteStudent(Connection con) throws SQLException {
        System.out.print("Enter student ID to delete: ");
        int id = Integer.parseInt(sc.nextLine());

        String sql = "DELETE FROM students WHERE student_id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Student deleted successfully!");
            else
                System.out.println("Student not found.");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println(
                    "Cannot delete: this student has course enrollments.");
        }
    }

    // 5. View Courses
    static void viewCourses(Connection con) throws SQLException {
        String sql = "SELECT * FROM courses";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nCourse ID\tCourse Name\t\tDuration");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("course_id") + "\t\t" +
                        rs.getString("course_name") + "\t\t" +
                        rs.getInt("duration_months") + " months");
            }
        }
    }

    // 6. Course Enrollment - TRANSACTION
    static void enrollStudent(Connection con) throws SQLException {
        System.out.print("Enter student ID: ");
        int studentId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter course ID: ");
        int courseId = Integer.parseInt(sc.nextLine());

        String sql = "INSERT INTO enrollments " +
                "(student_id, course_id, enrollment_date) " +
                "VALUES (?, ?, ?)";

        boolean oldAutoCommit = con.getAutoCommit();

        try {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                ps.setInt(2, courseId);
                ps.setDate(3, Date.valueOf(LocalDate.now()));

                ps.executeUpdate();
            }

            con.commit();
            System.out.println("Course enrollment successful!");

        } catch (SQLException e) {
            con.rollback();
            System.out.println(
                    "Enrollment failed. Check student/course IDs " +
                    "or whether the student is already enrolled.");
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    // 7. Attendance Management
    static void markAttendance(Connection con) throws SQLException {
        System.out.print("Enter student ID: ");
        int studentId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter course ID: ");
        int courseId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter status (Present/Absent): ");
        String status = sc.nextLine();

        if (!status.equalsIgnoreCase("Present")
                && !status.equalsIgnoreCase("Absent")) {
            System.out.println("Status must be Present or Absent.");
            return;
        }

        String sql = "INSERT INTO attendance " +
                "(student_id, course_id, attendance_date, status) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.setString(4,
                    status.substring(0, 1).toUpperCase()
                    + status.substring(1).toLowerCase());

            ps.executeUpdate();
            System.out.println("Attendance recorded successfully!");
        } catch (SQLException e) {
            System.out.println(
                    "Attendance failed. Ensure the student is enrolled.");
        }
    }

    // 8. Marks Entry
    static void enterMarks(Connection con) throws SQLException {
        System.out.print("Enter student ID: ");
        int studentId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter course ID: ");
        int courseId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter marks obtained: ");
        double marks = Double.parseDouble(sc.nextLine());

        System.out.print("Enter maximum marks: ");
        double maxMarks = Double.parseDouble(sc.nextLine());

        if (maxMarks <= 0 || marks < 0 || marks > maxMarks) {
            System.out.println("Invalid marks.");
            return;
        }

        String sql = "INSERT INTO marks " +
                "(student_id, course_id, marks_obtained, max_marks) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "marks_obtained=VALUES(marks_obtained), " +
                "max_marks=VALUES(max_marks)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.setDouble(3, marks);
            ps.setDouble(4, maxMarks);

            ps.executeUpdate();
            System.out.println("Marks saved successfully!");
        } catch (SQLException e) {
            System.out.println(
                    "Marks entry failed. Ensure the student is enrolled.");
        }
    }

    // 9. Result Generation - JOIN
    static void generateResult(Connection con) throws SQLException {
        System.out.print("Enter student ID: ");
        int studentId = Integer.parseInt(sc.nextLine());

        String sql = "SELECT s.student_name, c.course_name, " +
                "m.marks_obtained, m.max_marks, " +
                "ROUND(m.marks_obtained * 100.0 / m.max_marks, 2) " +
                "AS percentage " +
                "FROM students s " +
                "JOIN marks m ON s.student_id = m.student_id " +
                "JOIN courses c ON m.course_id = c.course_id " +
                "WHERE s.student_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    double percentage = rs.getDouble("percentage");

                    System.out.println("\nStudent: " +
                            rs.getString("student_name"));
                    System.out.println("Course: " +
                            rs.getString("course_name"));
                    System.out.println("Marks: " +
                            rs.getDouble("marks_obtained") + " / " +
                            rs.getDouble("max_marks"));
                    System.out.println("Percentage: " +
                            percentage + "%");
                    System.out.println("Result: " +
                            (percentage >= 40 ? "PASS" : "FAIL"));
                }

                if (!found)
                    System.out.println("No marks found for this student.");
            }
        }
    }

    // 10. Student and Course Details - JOIN
    static void viewStudentCourseDetails(Connection con)
            throws SQLException {

        String sql = "SELECT s.student_id, s.student_name, " +
                "c.course_name, e.enrollment_date " +
                "FROM students s " +
                "JOIN enrollments e ON s.student_id = e.student_id " +
                "JOIN courses c ON e.course_id = c.course_id " +
                "ORDER BY s.student_id";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nID\tStudent\t\tCourse\t\t\tEnrollment Date");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("student_id") + "\t" +
                        rs.getString("student_name") + "\t\t" +
                        rs.getString("course_name") + "\t\t" +
                        rs.getDate("enrollment_date"));
            }
        }
    }
}
