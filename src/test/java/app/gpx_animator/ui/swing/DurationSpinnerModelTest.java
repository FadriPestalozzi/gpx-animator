package app.gpx_animator.ui.swing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DurationSpinnerModelTest {

    private static Stream<Arguments> generateGetterSetterTestData() {
        return Stream.of(
                Arguments.of(Long.MIN_VALUE),
                Arguments.of(Long.MAX_VALUE),
                Arguments.of(1L),
                Arguments.of(0L),
                Arguments.of(100L),
                Arguments.of((Long) null)
        );
    }

    @ParameterizedTest
    @MethodSource("generateGetterSetterTestData") //NON-NLS
    void testValue(final Long value) {
        final var testee = new DurationSpinnerModel();
        testee.setValue(value);
        assertEquals(value, testee.getValue());
    }

    @ParameterizedTest
    @EnumSource(DurationSpinnerModel.Field.class)
    void testField(final DurationSpinnerModel.Field field) {
        final var testee = new DurationSpinnerModel();
        testee.setField(field);
        assertEquals(field, testee.getField());
    }

    @Test
    void testFromUnit() {
        assertEquals(DurationSpinnerModel.Field.MILLISECOND, DurationSpinnerModel.Field.fromUnit("ms")); //NON-NLS
    }
}
