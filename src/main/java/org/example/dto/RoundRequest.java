package org.example.dto;

public class RoundRequest {
    private int matchId;
    private int userId;
    private int round;

    public RoundRequest(int matchId, int userId, int round) {
        this.matchId = matchId;
        this.userId = userId;
        this.round = round;
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }
}
