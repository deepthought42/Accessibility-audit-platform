package com.looksee.browsing;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

/**
 * Created by Glib_Briia on 17/06/2016.
 */
public class Coordinates {

    private final int width;
    private final int height;
    private final int x;
    private final int y;

    public Coordinates(Point point, Dimension size, Double devicePixelRatio) {
        this.width = (int)(size.getWidth()*devicePixelRatio);
        this.height = (int)(size.getHeight()*devicePixelRatio);
        this.x = (int)(point.getX()*devicePixelRatio);
        this.y = (int)(point.getY()*devicePixelRatio);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
