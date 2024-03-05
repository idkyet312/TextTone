package com.example.texttone;

import java.util.List;

public class Alternative {
    private String transcript;
    private double confidence;
    public List<Word> words;

    // Getters
    public String getTranscript() {
        return transcript;
    }

    public double getConfidence() {
        return confidence;
    }

    public List<Word> getWords() {
        return words;
    }

    // Setters
    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }
}
