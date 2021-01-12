package app.gpx_animator.core.renderer.plugins;

import app.gpx_animator.core.configuration.Configuration;
import app.gpx_animator.core.renderer.RendererPlugin;
import app.gpx_animator.core.renderer.RenderingContext;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static app.gpx_animator.core.util.RenderUtil.getGraphics;

@SuppressWarnings("unused") // Plugins are loaded using reflection
public final class BackgroundColorPlugin implements RendererPlugin {

    private final transient Color backgroundColor;

    public BackgroundColorPlugin(@NonNull final Configuration configuration) {
        this.backgroundColor = configuration.getBackgroundColor();
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void renderBackground(@NonNull final BufferedImage image, @NonNull final RenderingContext context) {
        final var graphics = getGraphics(image);
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    @Override
    public void renderFrame(final int frame, @NonNull final BufferedImage image, @NonNull final RenderingContext context) { }

}
