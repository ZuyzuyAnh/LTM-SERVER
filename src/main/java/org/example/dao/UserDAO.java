package org.example.dao;

import org.example.dto.HistoryResponse;
import org.example.dto.LeaderboardResponse;
import org.example.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO extends DAO {

    public User createUser(String username, String password, String email, String role) throws SQLException {
        String query = "INSERT INTO users (username, password, email, role, score) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);  // Set email
            stmt.setString(4, role);
            stmt.setInt(5, 0);
            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int userId = generatedKeys.getInt(1);
                return new User(userId, username, password, email, role, 0);  // Return new user with email
            } else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }
        }
    }


    public List<User> getUsers() throws SQLException {
        String query = "SELECT * FROM users ORDER BY score DESC";
        List<User> users = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getInt("score")
                );

                users.add(user);
            }
        }

        return users;
    }


    public User getUser(int id) throws SQLException {
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getInt("score")
                );
            } else {
                System.out.println("User not found.");
                return null;
            }
        }
    }

    public User getUserByName(String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String user = rs.getString("username");
                String password = rs.getString("password");
                String email = rs.getString("email");
                String role = rs.getString("role");
                int score = rs.getInt("score");

                return new User(id, user, password, email, role, score);
            }else {
                throw new SQLException("User not found.");
            }
        }
    }


    public void updateUser(int id, String username, String role, int score) throws SQLException {
        String query = "UPDATE users SET username = ?, role = ?, score = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, role);
            stmt.setInt(3, score);
            stmt.setInt(4, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("User updated successfully.");
            } else {
                System.out.println("User not found.");
            }
        }
    }

    public void updateUserMatch(int userId, int matchId, int newScore) {
        String sql = "UPDATE user_match SET correct_answers = ? WHERE user_id = ? AND match_id = ?";

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, newScore);
            statement.setInt(2, userId);
            statement.setInt(3, matchId);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteUser(int id) throws SQLException {
        String query = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("User not found.");
            }
        }
    }

    public List<HistoryResponse> getUserMatchHistory(int userId) {
        String sql = "SELECT u1.username AS player_name, u2.username AS opponent_name, " +
                "um1.match_id, m.time AS match_time, " +
                "um1.correct_answers AS player_correct_answers, um1.status AS player_status " +
                "FROM user_match um1 " +
                "JOIN users u1 ON um1.user_id = u1.id " +
                "JOIN matchs m ON um1.match_id = m.id " +
                "JOIN user_match um2 ON um1.match_id = um2.match_id AND um1.user_id != um2.user_id " +
                "JOIN users u2 ON um2.user_id = u2.id " +
                "WHERE u1.id = ? " +
                "ORDER BY m.time DESC";

        List<HistoryResponse> historyList = new ArrayList<>();

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String user = resultSet.getString("player_name");
                String opponent = resultSet.getString("opponent_name");
                int matchId = resultSet.getInt("match_id");
                Timestamp matchTime = resultSet.getTimestamp("match_time");
                int scores = resultSet.getInt("player_correct_answers");
                String status = resultSet.getString("player_status");

                HistoryResponse history = new HistoryResponse(user, opponent, matchId, matchTime, scores, status);
                historyList.add(history);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return historyList;
    }

    public User findOpponent(int matchId, int userId) throws SQLException {
        String query = "SELECT u.id, u.username, u.password, u.email, u.role, u.score " +
                "FROM users u " +
                "JOIN user_match um ON u.id = um.user_id " +
                "WHERE um.match_id = ? AND u.id != ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, matchId);
            stmt.setInt(2, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getInt("score")
                );
            } else {
                System.out.println("Opponent not found.");
                return null;
            }
        }
    }

    public List<LeaderboardResponse> getLeaderboard() {
        String sql = "SELECT u.id, u.username, SUM(um.correct_answers) AS total_scores " +
                "FROM users u " +
                "JOIN user_match um ON u.id = um.user_id " +
                "GROUP BY u.id " +
                "ORDER BY total_scores DESC";

        List<LeaderboardResponse> leaderboard = new ArrayList<>();

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int userId = resultSet.getInt("id");
                String username = resultSet.getString("username");
                int totalScores = resultSet.getInt("total_scores");

                LeaderboardResponse user = new LeaderboardResponse(userId, username, totalScores);
                leaderboard.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return leaderboard;
    }

    public void updateUserMatchScore(int userId, int matchId, int score) throws SQLException {
        String sql = "UPDATE user_match SET correct_answers = ? WHERE user_id = ? AND match_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, score);     // Điểm mới của người dùng
            stmt.setInt(2, userId);    // ID người dùng
            stmt.setInt(3, matchId);   // ID trận đấu

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Score updated successfully for user " + userId + " in match " + matchId);
            } else {
                System.out.println("No matching record found to update score.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

}
  