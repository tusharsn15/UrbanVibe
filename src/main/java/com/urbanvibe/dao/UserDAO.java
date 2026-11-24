package com.urbanvibe.dao;

import com.urbanvibe.database.DatabaseConnection;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.model.Admin;
import com.urbanvibe.model.Customer;
import com.urbanvibe.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final String SELECT_ALL_COLUMNS =
            "SELECT user_id, name, email, password_hash, salt, phone, role, city, loyalty_points, employee_code FROM users ";

    public int insertUser(User user) throws DatabaseOperationException {
        String sql = "INSERT INTO users (name, email, password_hash, salt, phone, role, city, loyalty_points, employee_code) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getSalt());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole());
            ps.setString(7, user.getCity());
            if (user instanceof Customer) {
                ps.setInt(8, ((Customer) user).getLoyaltyPoints());
            } else {
                ps.setInt(8, 0);
            }
            if (user instanceof Admin) {
                ps.setString(9, ((Admin) user).getEmployeeCode());
            } else {
                ps.setNull(9, java.sql.Types.VARCHAR);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DatabaseOperationException("User was saved but no id was returned.");
        } catch (SQLException e) {
            if (DatabaseConnection.isConstraintViolation(e)) {
                throw new DatabaseOperationException("That email is already registered.", e);
            }
            throw new DatabaseOperationException("Could not save user: " + e.getMessage(), e);
        }
    }

    public User findByEmail(String email) throws DatabaseOperationException {
        String sql = SELECT_ALL_COLUMNS + "WHERE email = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not read user: " + e.getMessage(), e);
        }
    }

    public User findById(int userId) throws DatabaseOperationException {
        String sql = SELECT_ALL_COLUMNS + "WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not read user: " + e.getMessage(), e);
        }
    }

    public boolean emailExists(String email) throws DatabaseOperationException {
        return findByEmail(email) != null;
    }

    public List<Customer> getAllCustomers() throws DatabaseOperationException {
        String sql = SELECT_ALL_COLUMNS + "WHERE role = 'CUSTOMER' ORDER BY user_id";
        List<Customer> customers = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                customers.add((Customer) mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not list customers: " + e.getMessage(), e);
        }
        return customers;
    }

    public void updateProfile(User user) throws DatabaseOperationException {
        String sql = "UPDATE users SET name = ?, phone = ?, city = ? WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getCity());
            ps.setInt(4, user.getUserId());
            if (ps.executeUpdate() == 0) {
                throw new DatabaseOperationException("User not found.");
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not update profile: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        if (User.ROLE_ADMIN.equals(role)) {
            return new Admin(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"),
                    rs.getString("password_hash"), rs.getString("salt"), rs.getString("phone"),
                    rs.getString("city"), rs.getString("employee_code"));
        }
        return new Customer(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("salt"), rs.getString("phone"),
                rs.getString("city"), rs.getInt("loyalty_points"));
    }
}
