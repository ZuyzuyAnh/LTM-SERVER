package org.example.dao;

import org.example.model.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO extends DAO {

    public Question createQuestion(String soundUrl, String answer) throws SQLException {
        String query = "INSERT INTO questions (sound_url, answer) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, soundUrl);
            stmt.setString(2, answer);

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);

                    Question question = new Question(generatedId, soundUrl, answer);

                    // Return the newly created Question object
                    System.out.println("Question created successfully.");
                    return question;
                } else {
                    throw new SQLException("Creating question failed, no ID obtained.");
                }
            }
        }
    }


    public Question getQuestionById(int id) throws SQLException {
        String query = "SELECT * FROM questions WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Question(rs.getInt("id"), rs.getString("sound_url"), rs.getString("answer"));
            }
        }
        return null;
    }

    public List<Question> getAllQuestions() throws SQLException {
        List<Question> questions = new ArrayList<>();
        String query = "SELECT * FROM questions";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                questions.add(new Question(rs.getInt("id"), rs.getString("sound_url"), rs.getString("answer")));
            }
        }
        return questions;
    }

    public List<Question> getRandomQuestions() throws SQLException {
        List<Question> questions = new ArrayList<>();
        String query = "SELECT * FROM questions ORDER BY RAND() LIMIT 6";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                questions.add(new Question(rs.getInt("id"), rs.getString("sound_url"), rs.getString("answer")));
            }
        }
        return questions;
    }

    public void updateQuestion(int id, String soundUrl, String answer) throws SQLException {
        String query = "UPDATE questions SET sound_url = ?, answer = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, soundUrl);
            stmt.setString(2, answer);
            stmt.setInt(3, id);
            stmt.executeUpdate();
            System.out.println("Question updated successfully.");
        }
    }

    public void deleteQuestion(int id) throws SQLException {
        String query = "DELETE FROM questions WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("Question deleted successfully.");
        }
    }

    public Question getQuestionsByRoundAndMatch(int matchId, int round) throws SQLException {
        String query = "SELECT q.id, q.sound_url, q.answer " +
                "FROM question_match qm " +
                "JOIN questions q ON qm.question_id = q.id " +
                "WHERE qm.match_id = ? AND qm.round = ?";

        Question question = null;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, matchId);
            stmt.setInt(2, round);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String soundUrl = rs.getString("sound_url");
                    String answer = rs.getString("answer");

                    question = new Question(id, soundUrl, answer);
                }
            }
        }

        return question;
    }
}
