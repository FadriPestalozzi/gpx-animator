package app.gpx_animator;

import javax.swing.JComboBox;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

interface Calculation {
    double calc(final double kmh);
}

public enum SpeedUnit {
    KMH("km/h", kmh -> kmh),
    MPH("mph", kmh -> kmh * 0.62137119223733),
    MIN_KM("min/km", kmh -> 3600 / kmh / 60),
    MIN_MI("min/mi", kmh -> 3600 / (kmh * 0.62137119223733) / 60),
    KNOTS("kn", kmh -> kmh * 0.53995680346039),
    MACH("Ma", kmh -> kmh * 0.00081699346405229),
    LIGHT("c", kmh -> kmh * 9.2656693110598E-10);

    private final String abbreviation;

    private final Calculation calculation;

    private final transient ResourceBundle resourceBundle = Preferences.getResourceBundle();

    public static void fillComboBox(final JComboBox<SpeedUnit> comboBox) {
        Arrays.stream(values()).forEach(comboBox::addItem);
    }

    /**
     * Define a speed unit.
     *
     * @param abbreviation the abbreviation of the speed unit
     * @param calculation how to calculate the speed, based on KMH
     */
    SpeedUnit(final String abbreviation, final Calculation calculation) {
        this.abbreviation = abbreviation;
        this.calculation = calculation;
    }

    @Override
    public String toString() {
        return resourceBundle.getString("speedunit.".concat(name().toLowerCase(Locale.getDefault())));
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public double convertSpeed(final double speed) {
        return calculation.calc(speed);
    }
}
