package org.kingdoms.utils;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class TextureAtlas {
    private int rows, columns;
    private int width, height;
    private BufferedImage[][] spirites;
    private Map<String, BufferedImage> mappedSpirites;
    private final Map<BorderSide, Integer> offsets = new EnumMap<>(BorderSide.class);
    private boolean processed;

    private void checkProcessed() {
        if (processed) throw new IllegalStateException("Image atlas has already been processed and cannot be changed");
    }

    public enum BorderSide {
        RIGHT, LEFT, TOP, BOTTOM
    }

    public TextureAtlas rows(int rows) {
        if (rows <= 0) throw new IllegalArgumentException("Number of rows must be positive: " + rows);
        checkProcessed();
        this.rows = rows;
        return this;
    }

    public TextureAtlas columns(int columns) {
        if (columns <= 0) throw new IllegalArgumentException("Number of columns must be positive: " + columns);
        checkProcessed();
        this.columns = columns;
        return this;
    }

    /**
     * A greater offset moves the border closer to the center of the image.
     *
     * @param offset Can be negative.
     */
    public TextureAtlas offset(BorderSide side, int offset) {
        checkProcessed();
        this.offsets.put(side, offset);
        return this;
    }

    private int getOffset(BorderSide side) {
        return this.offsets.getOrDefault(side, 0);
    }

    public int getAdjustedHeight() {
        return this.height - getOffset(BorderSide.BOTTOM);
    }

    public int getAdjustedWidth() {
        return this.width - getOffset(BorderSide.RIGHT);
    }

    public TextureAtlas withSheet(BufferedImage image) {
        checkProcessed();
        if (rows <= 0) throw new IllegalStateException("No rows set: " + rows);
        if (columns <= 0) throw new IllegalStateException("No columns set: " + columns);

        if (height == 0 && width == 0) {
            height = image.getHeight() / rows;
            width = image.getWidth() / columns;
        }

        this.spirites = new BufferedImage[rows][columns];
        this.mappedSpirites = new HashMap<>(rows * columns);

        Objects.requireNonNull(image, "Cannot load null image sheet");
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                // Top-left-corner x, y
                // The height is the amount of y that gets added to the top-left-corner y, lower parts of an image have a greater y
                int x = (col * width) + getOffset(BorderSide.LEFT);
                int y = (row * height) + getOffset(BorderSide.TOP);
                int width = getAdjustedWidth();
                int height = getAdjustedHeight();

                try {
                    BufferedImage spirite = image.getSubimage(
                            x,
                            y,
                            width,
                            height
                    );
                    this.spirites[row][col] = spirite;
                } catch (Exception ex) {
                    throw new IllegalStateException("Failed to load spirit: x=" + x + ", y=" + y
                            + ", width=" + width + ", height=" + height + " dims=" + image.getWidth() + ", " + image.getHeight(), ex);
                }
            }
        }

        processed = true;
        return this;
    }

    public BufferedImage get(int row, int column) {
        return spirites[row][column];
    }

    public BufferedImage get(String name) {
        return mappedSpirites.get(name);
    }

    public TextureAtlas map(String name, int row, int column) {
        mappedSpirites.put(name, get(row, column));
        return this;
    }

    public TextureAtlas mapRow(int row, String... names) {
        int column = 0;

        for (String name : names) {
            map(name, row, column++);
        }

        return this;
    }
}
