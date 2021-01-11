package app.gpx_animator.core.renderer;

import app.gpx_animator.core.data.Position;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.awt.image.BufferedImage;

public final class ImageRenderer extends GraphicsRenderer {

    public void renderImage(@NonNull final BufferedImage image, @NonNull final Position position, final int margin,
                            @NonNull final BufferedImage targetImage) {
        if (Position.HIDDEN.equals(position)) {
            return;
        }

        final var imageWidth = image.getWidth();
        final var imageHeight = image.getHeight();
        final var targetImageWidth = targetImage.getWidth();
        final var targetImageHeight = targetImage.getHeight();
        final var graphics = getGraphics(targetImage);

        int xPosition;
        int yPosition;
        switch (position) {
            case TOP_LEFT -> {
                xPosition = margin;
                yPosition = margin;
            }
            case TOP_CENTER -> {
                xPosition = (targetImageWidth - imageWidth) / 2;
                yPosition = margin;
            }
            case TOP_RIGHT -> {
                xPosition = targetImageWidth - imageWidth - margin;
                yPosition = margin;
            }
            case BOTTOM_LEFT -> {
                xPosition = margin;
                yPosition = targetImageHeight - imageHeight - margin;
            }
            case BOTTOM_CENTER -> {
                xPosition = (targetImageWidth - imageWidth) / 2;
                yPosition = targetImageHeight - imageHeight - margin;
            }
            case BOTTOM_RIGHT -> {
                xPosition = targetImageWidth - imageWidth - margin;
                yPosition = targetImageHeight - imageHeight - margin;
            }
            default -> throw new IllegalStateException("Unexpected position: " + position);
        }
        graphics.drawImage(image, xPosition, yPosition, imageWidth, imageHeight, null);
    }

}
