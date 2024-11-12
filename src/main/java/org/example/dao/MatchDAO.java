package org.example.dao;

import org.example.model.Match;
import org.example.model.Question;

import java.sql.*;
import java.util.List;

public class MatchDAO extends DAO {

    public Match createMatch(Timestamp time, int player1Id, int player2Id, List<Question> questions) throws SQLException {
        String matchQuery = "INSERT INTO matchs (time) VALUES (?)";
        String userMatchQuery = "INSERT INTO user_match (user_id, match_id, correct_answers) VALUES (?, ?, ?)";
        String questionMatchQuery = "INSERT INTO question_match (question_id, match_id, round) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement matchStmt = conn.prepareStatement(matchQuery, Statement.RETURN_GENERATED_KEYS)) {
                matchStmt.setTimestamp(1, time);
                matchStmt.executeUpdate();

                ResultSet generatedKeys = matchStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int matchId = generatedKeys.getInt(1);
                    Match newMatch = new Match(matchId, time);

                    try (PreparedStatement userMatchStmt = conn.prepareStatement(userMatchQuery)) {
                        userMatchStmt.setInt(1, player1Id);
                        userMatchStmt.setInt(2, matchId);
                        userMatchStmt.setInt(3, 0);
                        userMatchStmt.executeUpdate();

                        userMatchStmt.setInt(1, player2Id);
                        userMatchStmt.setInt(2, matchId);
                        userMatchStmt.setInt(3, 0);
                        userMatchStmt.executeUpdate();
                    }

                    try (PreparedStatement questionMatchStmt = conn.prepareStatement(questionMatchQuery)) {
                        int round = 1; // Start with round 1, increment for each round
                        for (Question question : questions) {
                            questionMatchStmt.setInt(1, question.getId());
                            questionMatchStmt.setInt(2, matchId);
                            questionMatchStmt.setInt(3, round);
                            questionMatchStmt.addBatch();
                            round++;
                        }
                        questionMatchStmt.executeBatch();
                    }

                    conn.commit();
                    return newMatch;
                } else {
                    throw new SQLException("Creating match failed, no ID obtained.");
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }



    public void readMatches() throws SQLException {
        String query = "SELECT * FROM matchs";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                        ", Time: " + rs.getTimestamp("time"));
            }
        }
    }

    public void updateMatch(int id, Timestamp time) throws SQLException {
        String query = "UPDATE matchs SET time = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setTimestamp(1, time);
            stmt.setInt(2, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Match updated successfully.");
            } else {
                System.out.println("Match not found.");
            }
        }
    }

    public void deleteMatch(int id) throws SQLException {
        String query = "DELETE FROM matchs WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Match deleted successfully.");
            } else {
                System.out.println("Match not found.");
            }
        }
    }
}
