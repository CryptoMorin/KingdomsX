package org.kingdoms.services.maps.abstraction.markers;

import org.kingdoms.utils.internal.reflection.Reflect;

import java.awt.*;

public final class LandMarkerSettings {
    // public static int zoomMin = 1, zoomMax = 1;

    private Color fillColor;
    private Color lineColor;
    private int lineWidth;
    private String label, clickDescription, hoverDescription;
    private int zoomMin, zoomMax;
    private boolean specialFlag;
    private Integer priority;

    public LandMarkerSettings fillColor(Color fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    public LandMarkerSettings priority(int priority) {
        this.priority = priority;
        return this;
    }

    public LandMarkerSettings stroke(Color lineColor, int lineWidth) {
        this.lineColor = lineColor;
        this.lineWidth = lineWidth;
        return this;
    }

    public LandMarkerSettings label(String label) {
        this.label = label;
        return this;
    }

    public LandMarkerSettings clickDescription(String description) {
        this.clickDescription = description;
        return this;
    }

    public LandMarkerSettings hoverDescription(String description) {
        this.hoverDescription = description;
        return this;
    }

    public LandMarkerSettings zoom(int zoomMin, int zoomMax) {
        this.zoomMin = zoomMin;
        this.zoomMax = zoomMax;
        return this;
    }

    public LandMarkerSettings specialFlag(boolean specialFlag) {
        this.specialFlag = specialFlag;
        return this;
    }

    public Integer getPriority() {
        return priority;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public boolean getSpecialFlag() {
        return specialFlag;
    }

    public int getZoomMin() {
        return zoomMin;
    }

    public int getZoomMax() {
        return zoomMax;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public String getClickDescription() {
        return clickDescription;
    }

    public String getHoverDescription() {
        return hoverDescription;
    }

    @Override
    public String toString() {
        return Reflect.toString(this);
    }
}
