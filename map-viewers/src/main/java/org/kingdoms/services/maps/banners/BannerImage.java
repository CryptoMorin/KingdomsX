package org.kingdoms.services.maps.banners;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPatternType;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.kingdoms.services.maps.MapViewerAddon;
import org.kingdoms.utils.Banners;
import org.kingdoms.utils.TextureAtlas;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Base64;
import java.util.List;

public final class BannerImage {
    private static final TextureAtlas BANNER_PATTERN_ATLAS = new TextureAtlas()
            .rows(8) // The last two are empty
            .columns(8)
            // I'm not sure if there's a wiki for these offsets.
            .offset(TextureAtlas.BorderSide.BOTTOM, 23)
            .offset(TextureAtlas.BorderSide.RIGHT, 44)
            .offset(TextureAtlas.BorderSide.LEFT, 1)
            .withSheet(loadAtlas());

    private static BufferedImage loadAtlas() {
        // We use Minecraft's procedurally-generated texture atlas (generated using F3+S in-game.)
        // There are several atlas of the same type generated, we pick the one with the highest resolution.
        // https://minecraft.fandom.com/wiki/Banner_patterns.png-atlas
        // https://minecraft.fandom.com/wiki/Texture_atlas
        // We'd have to manually generate this again if Minecraft updates their patterns.
        // Last generated using Minecraft version v1.21.4
        // TODO Recreate all the patterns in Adobe Illustrator.
        // TODO We could also generate SVGs for better performance since the patterns are known, but
        //      that'll require an external library and more code. Needs some time to do this. This way
        //      we can embed the SVG directly as well without needing to save the image as Base64.
        InputStream atlas = MapViewerAddon.get().getResource("atlas/minecraft_textures_atlas_banner_patterns.png_0.png");
        try {
            return ImageIO.read(atlas);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run Minecraft's banner patterns atlas", e);
        }
    }

    static {
        // Minecraft generates a spirite mapping file as well, but it's easier to manage this way.
        mapRow(0, XPatternType.BASE, XPatternType.BORDER, XPatternType.CREEPER, XPatternType.CURLY_BORDER,
                XPatternType.GUSTER, XPatternType.HALF_VERTICAL_RIGHT, XPatternType.SKULL, XPatternType.SQUARE_TOP_LEFT);

        mapRow(1, XPatternType.BRICKS, XPatternType.CIRCLE, XPatternType.CROSS, XPatternType.DIAGONAL_LEFT,
                XPatternType.HALF_HORIZONTAL, XPatternType.MOJANG, XPatternType.SMALL_STRIPES, XPatternType.SQUARE_TOP_RIGHT);

        mapRow(2, XPatternType.DIAGONAL_RIGHT, XPatternType.DIAGONAL_UP_LEFT, XPatternType.DIAGONAL_UP_RIGHT, XPatternType.FLOW,
                XPatternType.HALF_HORIZONTAL_BOTTOM, XPatternType.PIGLIN, XPatternType.SQUARE_BOTTOM_LEFT, XPatternType.STRAIGHT_CROSS);

        mapRow(3, XPatternType.FLOWER, XPatternType.GLOBE, XPatternType.GRADIENT, XPatternType.GRADIENT_UP, XPatternType.HALF_VERTICAL,
                XPatternType.RHOMBUS, XPatternType.SQUARE_BOTTOM_RIGHT, XPatternType.STRIPE_BOTTOM);

        mapRow(4, XPatternType.STRIPE_CENTER, XPatternType.STRIPE_DOWNLEFT, XPatternType.STRIPE_DOWNRIGHT, XPatternType.STRIPE_LEFT,
                XPatternType.STRIPE_MIDDLE, XPatternType.STRIPE_RIGHT, XPatternType.STRIPE_TOP, XPatternType.TRIANGLE_BOTTOM);

        mapRow(5, XPatternType.TRIANGLE_TOP, XPatternType.TRIANGLES_BOTTOM, XPatternType.TRIANGLES_TOP);
        BANNER_PATTERN_ATLAS.mapRow(5, "banner_base", "missingno"); // We're not going to be using these tho.
    }

    private static void mapRow(int row, XPatternType... types) {
        int column = 0;
        for (XPatternType type : types) {
            BANNER_PATTERN_ATLAS.map(type.name(), row, column++);
        }
    }

    /**
     * Recolors all the pixels while preserving the alpha component,
     * this way, transparent pixels are colored too, but they'll remain
     * the same visually.
     * Minecraft uses the alpha component to create gradients.
     */
    private static void colorImage(BufferedImage img, Color color) {
        WritableRaster raster = img.getRaster();
        int[] pixel = {color.getRed(), color.getGreen(), color.getBlue()};

        for (int x = 0; x < raster.getWidth(); x++)
            for (int y = 0; y < raster.getHeight(); y++)
                for (int b = 0; b < pixel.length; b++)
                    raster.setSample(x, y, b, pixel[b]);
    }

    /**
     * Performs a deep copy on this image.
     * Works for {@link BufferedImage#getSubimage(int, int, int, int)} as well.
     */
    private static BufferedImage copy(BufferedImage image) {
        ColorModel cm = image.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster());
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private static BufferedImage resize(BufferedImage img, int width, int height) {
        // We don't need to maintain ratio, we're just scaling.
        // int originalWidth = img.getWidth();
        // int originalHeight = img.getHeight();
        // double ratio = Math.min((double) width / originalWidth, (double) height / originalHeight);
        // width = (int) (originalWidth * ratio);
        // height = (int) (originalHeight * ratio);

        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(width, height, img.getType());

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    protected static BufferedImage bannerToImage(XMaterial baseBanner, List<Pattern> patterns, int scaling) {
        int width = BANNER_PATTERN_ATLAS.getAdjustedWidth() * scaling;
        int height = BANNER_PATTERN_ATLAS.getAdjustedHeight() * scaling;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphic = image.createGraphics();

        DyeColor baseColor = Banners.getBannerMaterialColor(baseBanner);
        graphic.setColor(new Color(baseColor.getColor().asARGB(), true));
        graphic.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphic.setColor(Color.red); // This normally shouldn't be used. We can detect wrong drawings if such thing happens.

        for (Pattern pattern : patterns) {
            XPatternType xPattern = XPatternType.of(pattern.getPattern());
            BufferedImage spirite = BANNER_PATTERN_ATLAS.get(xPattern.name());
            if (spirite == null) {
                // Future-proof for updates
                spirite = BANNER_PATTERN_ATLAS.get("missingno");
            }

            spirite = resize(copy(spirite), width, height);

            DyeColor dyeColor = pattern.getColor();
            Color color = new Color(dyeColor.getColor().asARGB(), true);

            // graphic.setColor(color); This won't work.
            colorImage(spirite, color);
            graphic.drawImage(spirite, 0, 0, null);
        }

        graphic.dispose();
        return image;
    }

    public static String embedImgSrc(RenderedImage img, String formatName) {
        return "data:image/" + formatName + ";charset=utf-8;base64," + imgToBase64String(img, formatName);
    }

    public static String imgToBase64String(RenderedImage img, String formatName) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (OutputStream b64os = Base64.getEncoder().wrap(os)) {
            ImageIO.write(img, formatName, b64os);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return os.toString();
    }
}
