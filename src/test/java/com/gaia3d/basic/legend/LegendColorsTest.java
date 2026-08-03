package com.gaia3d.basic.legend;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("release")
class LegendColorsTest {
    @Test
    void getColorByBandUsesFloorLegendEntryAtBoundaries() {
        LegendColors legendColors = new LegendColors();
        GaiaColor low = new GaiaColor(1.0f, 0.0f, 0.0f, 1.0f);
        GaiaColor middle = new GaiaColor(0.0f, 1.0f, 0.0f, 1.0f);
        GaiaColor high = new GaiaColor(0.0f, 0.0f, 1.0f, 1.0f);
        legendColors.setValueAndColor(10.0, low);
        legendColors.setValueAndColor(20.0, middle);
        legendColors.setValueAndColor(30.0, high);

        assertSame(low, legendColors.getColorByBand(5.0));
        assertSame(low, legendColors.getColorByBand(10.0));
        assertSame(low, legendColors.getColorByBand(19.999));
        assertSame(middle, legendColors.getColorByBand(20.0));
        assertSame(middle, legendColors.getColorByBand(29.999));
        assertSame(high, legendColors.getColorByBand(30.0));
        assertSame(high, legendColors.getColorByBand(100.0));
    }
}
