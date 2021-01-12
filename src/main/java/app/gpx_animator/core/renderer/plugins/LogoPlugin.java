package app.gpx_animator.core.renderer.plugins;

import app.gpx_animator.core.UserException;
import app.gpx_animator.core.configuration.Configuration;
import app.gpx_animator.core.data.Position;
import app.gpx_animator.core.renderer.ImageRenderer;
import app.gpx_animator.core.renderer.RendererPlugin;
import app.gpx_animator.core.renderer.RenderingContext;
import edu.umd.cs.findbugs.annotations.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@SuppressWarnings("unused") // Plugins are loaded using reflection
public final class LogoPlugin extends ImageRenderer implements RendererPlugin {

    private final transient BufferedImage logo;
    private final transient Position position;
    private final transient int margin;

    public LogoPlugin(@NonNull final Configuration configuration) throws UserException {
        position = configuration.getLogoPosition();
        margin = configuration.getLogoMargin();

        final var file = configuration.getLogo();
        if (file != null && file.exists()) {
            try {
                logo = ImageIO.read(file);
            } catch (final IOException e) {
                throw new UserException("Can't read logo: ".concat(e.getMessage())); // TODO translate
            }
        } else {
            logo = null;
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void renderBackground(@NonNull final BufferedImage image, @NonNull final RenderingContext context) { }

    @Override
    public void renderFrame(final int frame, final @NonNull BufferedImage image, final @NonNull RenderingContext context) {
        if (logo == null || position.equals(Position.HIDDEN)) {
            // no logo defined or logo should not be visible
            return;
        }

        renderImage(logo, position, margin, image);
    }

}
