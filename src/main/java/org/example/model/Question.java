package org.example.model;

public class Question {
    private int id;
    private String soundUrl;
    private String answer;

    public Question(int id, String soundUrl, String answer) {
        this.id = id;
        this.soundUrl = soundUrl;
        this.answer = answer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSoundUrl() {
        return soundUrl;
    }

    public void setSoundUrl(String soundUrl) {
        this.soundUrl = soundUrl;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

}
