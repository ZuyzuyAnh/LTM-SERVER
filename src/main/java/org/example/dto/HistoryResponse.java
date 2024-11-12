package org.example.dto;

import java.sql.Timestamp;

public class HistoryResponse {
    private String user;
    private String opponent;
    private int matchId;
    private Timestamp time;
    private int scores;
    private String status;

    public HistoryResponse(String user, String opponent, int matchId, Timestamp time, int scores, String status) {
        this.user = user;
        this.opponent = opponent;
        this.matchId = matchId;
        this.time = time;
        this.scores = scores;
        this.status = status;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getOpponent() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public int getScores() {
        return scores;
    }

    public void setScores(int scores) {
        this.scores = scores;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
