package org.example.dto;

public class AnswerRequest {
    private String answer;
    private int matchId;
    private int round;
    private float time;

    public AnswerRequest(String answer, int matchId, int round, float time) {
        this.answer = answer;
        this.matchId = matchId;
        this.round = round;
        this.time = time;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }
}
