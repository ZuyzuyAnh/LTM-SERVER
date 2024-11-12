package org.example.dto;

public class LeaderboardResponse {
    private int userId;
    private String username;
    private int totalScores;

    public LeaderboardResponse(int userId, String username, int totalScores) {
        this.userId = userId;
        this.username = username;
        this.totalScores = totalScores;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalScores() {
        return totalScores;
    }

    public void setTotalScores(int totalScores) {
        this.totalScores = totalScores;
    }
}
