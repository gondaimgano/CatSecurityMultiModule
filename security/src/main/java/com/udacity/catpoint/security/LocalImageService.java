package com.udacity.catpoint.security;

import java.awt.image.BufferedImage;
import java.util.Random;

public class LocalImageService {
    private final Random r = new Random();

    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold) {
        return r.nextBoolean();
    }
}
