package com.udacity.stockhawk.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import static com.udacity.stockhawk.ui.WidgetDataProvider.cursorData;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;
    Parcelable mLayoutManagerSavedState;
    LinearLayoutManager mLinearManager;
    String SavedLinearManager;
    private StockAdapter adapter;
    public static String SYMBOL_DATA;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        adapter = new StockAdapter(this, this);
        stockRecyclerView.setAdapter(adapter);

        if (savedInstanceState != null) {
            mLayoutManagerSavedState = savedInstanceState.getParcelable(SavedLinearManager);
        }

        mLinearManager = new LinearLayoutManager(this);
        stockRecyclerView.setLayoutManager(mLinearManager);

        if (mLayoutManagerSavedState != null) {
            mLinearManager.onRestoreInstanceState(mLayoutManagerSavedState);
        }

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();

        QuoteSyncJob.initialize(this);
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());

               int returnDeleteId = getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
                PrefUtils.removeStock(MainActivity.this, symbol);

			   if(returnDeleteId > 0) {
                   new UpdateWidgetAsync().execute();
			   }

            }
        }).attachToRecyclerView(stockRecyclerView);


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (stockRecyclerView.getLayoutManager() != null) {
            outState.putParcelable(SavedLinearManager, stockRecyclerView.getLayoutManager().onSaveInstanceState());
        }
    }


    /**
     * Class to update widget upon an item deletion
     */
    public class UpdateWidgetAsync extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... params) {
            return getContentResolver()
                    .query(
                            Contract.Quote.URI,
                            null, null, null, null);

        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            super.onPostExecute(cursor);

            cursorData(cursor);

            /*notify and update the app-widget - 
			*for more details refer https://developer.android.com/reference/android/appwidget/AppWidgetManager.html and 
			*http://stackoverflow.com/a/24521503/7421148 
			*/
            Context context = getApplicationContext();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName stockHawkWidget = new ComponentName(context, CollectionWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(stockHawkWidget);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);


        }
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Override
    public void onRefresh() {

        QuoteSyncJob.syncImmediately(this);

        if (!networkUp() && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(this).size() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }


    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    public String msymbol;
    public Stock mStock;

    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {

            if (networkUp()) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

            msymbol = symbol;

            if (msymbol.contains(" ") || (msymbol.matches(".*\\W+.*") && !msymbol.matches("."))) {
                //Check for white space in User entered STOCK SYMBOL TO AVOID APP Crash
                Toast.makeText(MainActivity.this, getString(R.string.invalid_stock_no_space), Toast.LENGTH_LONG).show();
            } else {
                //check ticker symbol validity against a server
				 if (networkUp()) {
				 new AsyncStock().execute(msymbol); 
				 } else { 
				 String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();}
            }

        }
    }

    /**
     * Class to make an API call on the background thread to check for stock ticker symbol validity
     */
    public class AsyncStock extends AsyncTask<String, Void, Stock> {

        @Override
        protected Stock doInBackground(String... params) {

            Stock stockValue;
            try {
                stockValue = YahooFinance.get(msymbol);
                mStock = stockValue;

            } catch (IOException exception) {
                Timber.e(exception, "Error fetching stock quotes");
            }

            return mStock;
        }

        @Override
        protected void onPostExecute(Stock mStocks) {

            if (mStocks.toString().contains("null")) {
                QuoteSyncJob.syncImmediately(MainActivity.this);
                Toast.makeText(MainActivity.this, getString(R.string.invalid_stock), Toast.LENGTH_LONG).show();


            } else {

                PrefUtils.addStock(MainActivity.this, msymbol);
                QuoteSyncJob.syncImmediately(MainActivity.this);
                           }

        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);

    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }


    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(String symbol) {
        Intent intent = new Intent(this, GraphActivity.class);
        intent.putExtra(SYMBOL_DATA, symbol);
        startActivity(intent);

    }
}
