package com.nyu.cs9033.eta.controllers;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nyu.cs9033.eta.R;
import com.nyu.cs9033.eta.database.TripDatabaseHelper;
import com.nyu.cs9033.eta.models.Person;
import com.nyu.cs9033.eta.models.Trip;

public class TripHistoryActivity extends ListActivity
{
	TripDatabaseHelper tdh = new TripDatabaseHelper(this);
	private Button back;
	private static final String TAG = "TRIPHISTORYACTIVITY";
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		try
		{
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_trip_history_linear);
			
			back = (Button)findViewById(R.id.btnback);
			back.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					finish();
				}
			});
			
			String from[] = tdh.getColsTripHistoryList();
			int to[] = new int[]{R.id.tripid, R.id.tripname};
			
			Cursor c = tdh.getAllTrips();
			
			SimpleCursorAdapter sca = new SimpleCursorAdapter(this, R.layout.trip_history_row, c, from, to, 0);
			setListAdapter(sca);
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in onCreate :" + e.toString());
			Toast.makeText(this, "Exception : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
    public void onListItemClick(ListView l, View v, int position, long id) 
	{
		try
		{
			Intent intent = new Intent(this, ViewTripActivity.class);
			
			long trip_id = Long.parseLong(((TextView)v.findViewById(R.id.tripid)).getText().toString());
			Trip t = tdh.getTrip(trip_id);
			t.setTripId(trip_id);
			
			ArrayList<Person> p = new ArrayList<Person>();
			p = tdh.getTripPersons(trip_id);
			t.setFriends(p);
			
			Bundle b = new Bundle();
			b.putParcelable("trip", t);
			intent.putExtras(b);
			
	        startActivityForResult(intent, 1);
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in onListItemClick :" + e.toString());
			Toast.makeText(this, "Exception : "+e.toString(),Toast.LENGTH_LONG).show();
		}
    }
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		try
		{
			super.onActivityResult(requestCode, resultCode, data);
			if(resultCode==RESULT_OK)
			{
			   Intent refresh = new Intent(this, TripHistoryActivity.class);
			   startActivity(refresh);
			   this.finish();
			}
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in onActivityResult :" + e.toString());
			Toast.makeText(this, "Exception : "+e.toString(),Toast.LENGTH_LONG).show();
		}
    }
}
