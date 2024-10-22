package me.hxlixd;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.*;

public class AudioHandle {

    public AudioHandle(String fileName) {
        play(fileName + ".wav");
    }

    public void play(String soundFileName) {
        try {
            InputStream audioSrc = getClass().getResourceAsStream("/sounds/" + soundFileName);
            if (audioSrc == null) {
                throw new IllegalArgumentException("Sound file not found: " + soundFileName);
            }

            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                }
            });
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}