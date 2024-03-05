package com.example.texttone;

public class AudioProcessor {

    public static double calculateRMSVolume(byte[] audioBytes, int sampleRate, int bitDepth, int channels, double startTimeSeconds, double endTimeSeconds) {
        // Calculate the number of bytes per sample
        int bytesPerSample = bitDepth / 8;

        // Calculate the start index based on the start time
        int startIndex = (int) (sampleRate * startTimeSeconds * channels * bytesPerSample);

        // Calculate the end index based on the end time, ensuring it does not exceed the audioBytes length
        int endIndex = (int) (sampleRate * endTimeSeconds * channels * bytesPerSample);
        endIndex = Math.min(endIndex, audioBytes.length);

        // Calculate the number of bytes to process, adjusting for the actual end index
        int numBytes = endIndex - startIndex;

        // Ensure that the byte range is valid
        if (numBytes <= 0) {
            //endIndex += 5000;
            double rms = 1000;
            return rms;
            //throw new IllegalArgumentException("Invalid start or end time." + startIndex + " " + endIndex);
        }

        // Convert byte array to an array of shorts (16-bit audio data) starting from startIndex
        short[] audioData = new short[numBytes / 2];
        for (int i = startIndex, j = 0; i < endIndex - 1; i += 2, j++) {
            // Combine two bytes to form a short value
            audioData[j] = (short) (((audioBytes[i + 1] & 0xFF) << 8) | (audioBytes[i] & 0xFF));
        }

        // Calculate RMS volume
        long sumSquare = 0;
        for (short sample : audioData) {
            sumSquare += sample * sample;
        }
        double rms = Math.sqrt((double) sumSquare / audioData.length);

        // Return the RMS volume
        return rms;
    }
}
