package com.udacity.stockhawk.ui;


import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.ArrayList;

/**
 * Class to custom modify the x-axis labels
 * For more information - see the Official documentation here
 * https://github.com/PhilJay/MPAndroidChart/wiki/Setting-Data
 * https://github.com/PhilJay/MPAndroidChart/wiki/XAxis
 * https://github.com/PhilJay/MPAndroidChart/wiki/The-AxisValueFormatter-interface
 */
public class StockDateFormatter implements IAxisValueFormatter {

    private ArrayList<String> dateString;


    public StockDateFormatter(ArrayList<String> s) {
        dateString = s;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return dateString.get((int) value);
    }
}
