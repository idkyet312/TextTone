package com.example.texttone;

import java.util.List;

public class SpeechRecognitionResponse {
    private List<Result> results;

    // Getter
    public List<Result> getResults() {
        return results;
    }

    // Setter
    public void setResults(List<Result> results) {
        this.results = results;
    }
}
