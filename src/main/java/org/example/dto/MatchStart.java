package org.example.dto;

import org.example.model.Match;
import org.example.model.User;

import java.util.List;

public class MatchStart {
    private Match match;
    private List<QuestionAnswer> questionAnswers;

    public MatchStart(Match match, List<QuestionAnswer> questionAnswers) {
        this.match = match;
        this.questionAnswers = questionAnswers;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public List<QuestionAnswer> getQuestionAnswers() {
        return questionAnswers;
    }

    public void setQuestionAnswers(List<QuestionAnswer> questionAnswers) {
        this.questionAnswers = questionAnswers;
    }
}
