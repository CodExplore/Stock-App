package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.udacity.stockhawk.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import static com.udacity.stockhawk.ui.MainActivity.SYMBOL_DATA;


public class GraphActivity extends AppCompatActivity {

    @BindView(R.id.chart_graph)
    LineChart chart;
    String symbolData;
    ProgressBar mProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        ButterKnife.bind(this);


        Intent readSymbolIntent = getIntent();

        //Check if Intent has Extra and only then proceed with the processing
        if (readSymbolIntent.hasExtra(SYMBOL_DATA)) {

            symbolData = readSymbolIntent.getStringExtra(SYMBOL_DATA);

            if (networkUp()) {
                new AsyncStock().execute(symbolData);
            } else {
                String message = getString(R.string.no_network);
                Toast.makeText(GraphActivity.this, message, Toast.LENGTH_LONG).show();
            }

        }
    }

    /**
     * Method to check for network connectivity
     *
     * @return A Boolean type, true for network available and false for no network
     */
    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }


    /**
     * Class to query Yahoo Finance API in the background thread and set the data to the chart on the main thread
     */
    public class AsyncStock extends AsyncTask<String, Void, List<HistoricalQuote>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress();
        }

        @Override
        protected List<HistoricalQuote> doInBackground(String... params) {

            String mSymbol = params[0];

            Calendar from = Calendar.getInstance();
            Calendar to = Calendar.getInstance();
            from.add(Calendar.MONTH, -2); // from 2 month ago

            Stock returnStock = new Stock(mSymbol);
            try {
                returnStock = YahooFinance.get(mSymbol);
            } catch (IOException e) {

                e.printStackTrace();
            }

            List<HistoricalQuote> HistQuotes = null;
            try {
                HistQuotes = returnStock.getHistory(from, to, Interval.DAILY);
            } catch (IOException e) {

                e.printStackTrace();
            }

            return HistQuotes;
        }

        @Override
        protected void onPostExecute(List<HistoricalQuote> s) {
            super.onPostExecute(s);

            if (s.size() > 0) {
                List<Entry> entries = new ArrayList<Entry>();
                ArrayList<String> dates = new ArrayList<>();

                for (int i = 0; i < s.size(); i++) {
                //retrieve the epoch time in millis and format it in "yyyy-MM-dd" to display as X-axis labels
                    Date eDate = new Date(s.get(i).getDate().getTimeInMillis());
                    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
                    String closeDate = f.format(eDate);

                    dates.add(closeDate);
                    entries.add(new Entry(i, s.get(i).getClose().floatValue()));
                }
                LineDataSet dataSet = new LineDataSet(entries, getString(R.string.stock_close_price));
                dataSet.setDrawFilled(true);
                LineData lineData = new LineData(dataSet);

                XAxis xAxis = chart.getXAxis();
                xAxis.setGranularity(1f);
                xAxis.setValueFormatter(new StockDateFormatter(dates));

                //draw the chart with the available line data
                chart.setData(lineData);
                chart.setBackgroundColor(ContextCompat.getColor(GraphActivity.this, R.color.chart_background));
                chart.invalidate();

                TextView stockName = (TextView) findViewById(R.id.stock_name);
                stockName.setText(symbolData);

            } else {
                Log.i("GRAPHACTIVITY ", "Array is null");
            }
            hideProgress();
        }
    }

    /**
     * Method to show the progress bar and hide other views
     */
    public void showProgress() {

        LinearLayout graph_container = (LinearLayout) findViewById(R.id.graph_container);
        graph_container.setVisibility(View.INVISIBLE);

        TextView stockName = (TextView) findViewById(R.id.stock_name);
        stockName.setVisibility(View.INVISIBLE);

        chart.setVisibility(View.INVISIBLE);

        mProgress = (ProgressBar) findViewById(R.id.progress_indicator);
        mProgress.setVisibility(View.VISIBLE);
    }

    /**
     * Method to hide the progress bar and show the chart
     */
    public void hideProgress() {

        TextView stockName = (TextView) findViewById(R.id.stock_name);
        stockName.setVisibility(View.VISIBLE);

        chart.setVisibility(View.VISIBLE);

        mProgress = (ProgressBar) findViewById(R.id.progress_indicator);
        mProgress.setVisibility(View.INVISIBLE);

        LinearLayout graph_container = (LinearLayout) findViewById(R.id.graph_container);
        graph_container.setVisibility(View.VISIBLE);
    }
}
