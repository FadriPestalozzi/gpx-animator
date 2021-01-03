package app.gpx_animator;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class TextRenderer {

    private static final int IMAGE_TYPE = BufferedImage.TYPE_4BYTE_ABGR;
    private static final float STRIKE_WIDTH = 3f;
    private static final Stroke STROKE = new BasicStroke(STRIKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private final transient Font font;
    private final transient FontMetrics fontMetrics;

    public TextRenderer(@NonNull final Font font) {
        this.font = font;
        this.fontMetrics = getFontMetrics();
    }

    private FontMetrics getFontMetrics() {
        final var image = new BufferedImage(100, 100, IMAGE_TYPE);
        final var graphics = (Graphics2D) image.getGraphics();
        graphics.setStroke(STROKE);
        return graphics.getFontMetrics(font);
    }

    private int calculateTextWidth(@NonNull final String text) {
        return Arrays.stream(text.split("\n"))
                .mapToInt(fontMetrics::stringWidth)
                .max()
                .orElseThrow();
    }

    private int calculateTextHeight(@NonNull final String text) {
        final var lines = text.split("\n").length;
        return lines * fontMetrics.getHeight();
    }

    public BufferedImage renderText(@NonNull final String text, @NonNull final TextAlignment alignment) {
        final var trimmedText = text.trim();
        final var width = calculateTextWidth(trimmedText);
        final var height = calculateTextHeight(trimmedText);

        final var image = new BufferedImage(width, height, IMAGE_TYPE);
        final var graphics = (Graphics2D) image.getGraphics();
        graphics.setStroke(STROKE);
        graphics.setFont(font);

        final var fontRenderContext = graphics.getFontRenderContext();
        final var lineHeight = fontMetrics.getHeight();

        var lineNum = 0;
        for (final var line : trimmedText.split("\n")) {
            lineNum++;
            var xPosition = calculateHorizontalPosition(alignment, line, image.getWidth());
            var yPosition = calculateVerticalPosition(lineNum, lineHeight);

            final var textLayout = new TextLayout(line, font, fontRenderContext);
            final var shape = textLayout.getOutline(AffineTransform.getTranslateInstance(xPosition, yPosition));

            graphics.setColor(Color.white);
            graphics.fill(shape);
            graphics.draw(shape);

            graphics.setColor(Color.black);
            graphics.drawString(line, xPosition, yPosition);
        }

        return image;
    }

    private int calculateHorizontalPosition(@NonNull final TextAlignment alignment, @NonNull final String line, final int width) {
        return switch (alignment) {
            case LEFT -> 0;
            case CENTER -> (width - fontMetrics.stringWidth(line)) / 2;
            case RIGHT -> width - fontMetrics.stringWidth(line);
        };
    }

    private int calculateVerticalPosition(final int lineNum, final int lineHeight) {
        return (-fontMetrics.getHeight() + fontMetrics.getAscent()) + (lineNum * lineHeight);
    }

    public enum TextAlignment {
        LEFT, CENTER, RIGHT
    }
}
