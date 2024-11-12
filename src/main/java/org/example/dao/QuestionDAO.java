package org.example.dao;

import org.example.dto.QuestionAnswer;
import org.example.model.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
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
        String query;
        boolean updateSoundUrl = (soundUrl != null && !soundUrl.isEmpty());

        if (updateSoundUrl) {
            query = "UPDATE questions SET sound_url = ?, answer = ? WHERE id = ?";
        } else {
            query = "UPDATE questions SET answer = ? WHERE id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (updateSoundUrl) {
                stmt.setString(1, soundUrl);
                stmt.setString(2, answer);
                stmt.setInt(3, id);
            } else {
                stmt.setString(1, answer);
                stmt.setInt(2, id);
            }

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

    public List<QuestionAnswer> getQuestionsByMatch(int matchId) throws SQLException {
        String query = "SELECT q.id, q.sound_url, q.answer " +
                "FROM question_match qm " +
                "JOIN questions q ON qm.question_id = q.id " +
                "WHERE qm.match_id = ?";

        List<QuestionAnswer> questionAnswers = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, matchId);

            try (ResultSet rs = stmt.executeQuery()) {
                // Lặp qua tất cả các câu hỏi trong trận đấu
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String soundUrl = rs.getString("sound_url");
                    String answer = rs.getString("answer");

                    // Tạo đối tượng Question từ kết quả
                    Question question = new Question(id, soundUrl, answer);

                    // Lấy danh sách đáp án ngẫu nhiên
                    List<String> answers = getRandomAnswersExcludingCorrectAnswer(id);
                    answers.add(question.getAnswer());

                    // Thêm câu hỏi và các đáp án vào danh sách kết quả
                    Collections.shuffle(answers); // Trộn đáp án
                    QuestionAnswer questionAnswer = new QuestionAnswer(question, answers, question.getAnswer());
                    questionAnswers.add(questionAnswer);
                }
            }
        }

        return questionAnswers;
    }

    public List<String> getRandomAnswersExcludingCorrectAnswer(int questionId) throws SQLException {
        List<String> answers = new ArrayList<>();
        String query = "SELECT answer FROM questions WHERE id != ? ORDER BY RAND() LIMIT 9";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, questionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    answers.add(rs.getString("answer"));
                }
            }
        }

        return answers;
    }

}
