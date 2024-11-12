package org.example.dto;

public class EndRequest {
    private int finalScore;
    private int matchId;

    public EndRequest(int finalScore, int matchId) {
        this.finalScore = finalScore;
        this.matchId = matchId;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }
}
