package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * WidgetDataProvider acts as the adapter for the collection view widget,
 * providing RemoteViews to the widget in the getViewAt method.
 */
public class WidgetDataProvider implements RemoteViewsService.RemoteViewsFactory{

    private static final String TAG = "WidgetDataProvider";
	private static Cursor mCursor;
    private final DecimalFormat percentageFormat;
    private final DecimalFormat dollarFormatWithPlus;

    List<StockUtils> mCollection = new ArrayList<>();
    Context mContext = null;
    private final DecimalFormat dollarFormat;




    public WidgetDataProvider(Context context, Intent intent) {
        mContext = context;
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
 
    }



    @Override
    public void onCreate() {
        initData();
    }

    @Override
    public void onDataSetChanged() {
        initData();
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return mCollection.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews view = new RemoteViews(mContext.getPackageName(),
                R.layout.list_remote_view);
        view.setTextViewText(R.id.symbol_rv, mCollection.get(position).symbol_rv);
       view.setTextViewText(R.id.price_rv, mCollection.get(position).price_rv);
      view.setTextViewText(R.id.change_rv, mCollection.get(position).change_rv);

        if (mCollection.get(position).rawAbsoluteChange > 0) {
            view.setInt(R.id.change_rv, "setBackgroundResource", R.drawable.percent_change_pill_green);
        } else {
            view.setInt(R.id.change_rv, "setBackgroundResource", R.drawable.percent_change_pill_red);
        }
        return view;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }



	public static void cursorData(Cursor cursor){
	mCursor = cursor;

	}

    private void initData() {
		  int count = 0;
		  if (mCursor != null) {
            count = mCursor.getCount();
        
		
        mCollection.clear();
        for (int i = 0; i < count; i++) {
			StockUtils stockData = new StockUtils();
            mCursor.moveToPosition(i);
			stockData.symbol_rv = mCursor.getString(Contract.Quote.POSITION_SYMBOL);
			stockData.price_rv = (dollarFormat.format(mCursor.getFloat(Contract.Quote.POSITION_PRICE)));


            stockData.rawAbsoluteChange = mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);


            String change = dollarFormatWithPlus.format(stockData.rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);

            if (PrefUtils.getDisplayMode(mContext)
                    .equals(mContext.getString(R.string.pref_display_mode_absolute_key))) {
                stockData.change_rv = change;
            } else {
                stockData.change_rv = percentage;
            }


            mCollection.add(stockData);
        }
		}
    }



}