package org.example.dto;

public class MatchScore {
    private int player1Score;
    private int player2Score;
    private int player1Id;
    private int player2Id;

    public MatchScore(int player1Id, int player2Id) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.player1Score = -1;  // -1 indicates score not yet submitted
        this.player2Score = -1;
    }

    public void setScore(int userId, int score) {
        if (userId == player1Id) {
            this.player1Score = score;
        } else if (userId == player2Id) {
            this.player2Score = score;
        }
    }

    public boolean isBothScoresSubmitted() {
        return player1Score != -1 && player2Score != -1;
    }

    public int getWinnerId() {
        if (player1Score > player2Score) {
            return player1Id;
        } else if (player2Score > player1Score) {
            return player2Id;
        } else {
            return -1; // Tie
        }
    }
}
