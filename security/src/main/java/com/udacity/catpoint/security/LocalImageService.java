package com.udacity.catpoint.security;

import java.awt.image.BufferedImage;
import java.util.Random;

public interface LocalImageService {


    boolean imageContainsCat(BufferedImage image, float confidenceThreshhold) ;
}
