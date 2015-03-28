package com.nyu.cs9033.eta.controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.nyu.cs9033.eta.R;
import com.nyu.cs9033.eta.database.TripDatabaseHelper;
import com.nyu.cs9033.eta.helper.AsyncResponse;
import com.nyu.cs9033.eta.helper.LocationUpdateService;
import com.nyu.cs9033.eta.helper.SendPostRequestHelper;
import com.nyu.cs9033.eta.models.Person;
import com.nyu.cs9033.eta.models.Trip;


public class CreateTripActivity extends FragmentActivity implements AsyncResponse
{
	static Date date = new Date(System.currentTimeMillis());
	int frnd_num = 0;
	Trip t = null;
	private long responseId = -1;
	private static final int FRIENDNAMEIDPREFIX = 100000;
	private static final int FRIENDLOCATIONIDPREFIX = 200000;
	private static final int FRIENDPHONEIDPREFIX = 300000;
	private static final int FRIENDLAYOUTLNONEIDPREFIX = 900000;
	private static final int FRIENDLAYOUTLNTWOIDPREFIX = 910000;
	private static final int PICK_CONTACT = 1;
	private static final String RESPONSE_JSON_KEY = "trip_id";
	private static final String TAG = "CREATETRIPACTIVITY";
	public SendPostRequestHelper req = new SendPostRequestHelper();
	private String HW3API_URI = "location://com.example.nyu.hw3api";
	private Button btnfindplaces;
	private Button btncanceltrip;
	
	
	//*************Used from <http://developer.android.com/guide/topics/ui/controls/pickers.html> *************
	public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener 
	{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) 
		{
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			
			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, year, month, day);
		}
		
		public void onDateSet(DatePicker view, int year, int month, int day) 
		{
			// Do something with the date chosen by the user
			try
			{
				date = new SimpleDateFormat("MM/dd/yyyy").parse(month+"/"+day+"/"+year);
			}
			catch(Exception e)
			{
				Log.i(TAG, "Exception in onDateSet :" + e.toString());
			}
		}
	}
	public void showDatePickerDialog(View v) 
	{
	    DialogFragment newFragment = new DatePickerFragment();
	    newFragment.show(getSupportFragmentManager(), "datePicker");
	}
	//***********************************************************************************************************
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		try
		{
			super.onCreate(savedInstanceState);
			req.transferResult = this;
			setContentView(R.layout.activity_create_linear);
			this.btnfindplaces = (Button)findViewById(R.id.btnfindplace);
			btnfindplaces.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					getFourSqurDataActivity();
				
				}
			});
			
			btncanceltrip = (Button)findViewById(R.id.btncanceltrip);
			btncanceltrip.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					cancelTripCreation();
				}
			});
			if(getIntent().getAction() != null)
			{
				Intent intent = getIntent();
				String action = intent.getAction();
			    if (Intent.ACTION_SEND.equals(action) & intent.getType().equals("text/plain"))
			    {
			    	String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
			    	sharedText = sharedText.substring(0, sharedText.indexOf("http")).trim();
			    	EditText destination = (EditText) findViewById(R.id.editdestname);
			    	destination.setText(sharedText);
			    }
			}	
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in onCreate :" + e.toString());
			Toast.makeText(this, "Exception in onCreate : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	public void onClick(View v)
	{
		try
		{
			Button b = (Button)v;
			switch(b.getId())
			{
				case R.id.btncreatetrip:
					t = createTrip();
					if(t == null)
						break;
					if(isConnected())
						sendToServer();
					else
					{
						t.setServerRefId(0);
						Toast.makeText(this, "No Network Connection - Saving locally!",Toast.LENGTH_LONG).show();
						saveTrip(t);
					}
					Intent i = new Intent(this, LocationUpdateService.class);
					
					ComponentName x = startService(i);
					break;
				case R.id.btnadd:
					frnd_num++;
					addPersonToLayout();
					Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
			    	startActivityForResult(intent, PICK_CONTACT);
					break;
				case R.id.btndelete:
					deletePersonFromLayout();
					frnd_num--;
					break;
				
				
		
			}
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in inClick :" + e.toString());
			Toast.makeText(this, "Exception in onClick : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	public boolean isConnected()
	{
		try
		{
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = cm.getActiveNetworkInfo();
			return ni!=null && ni.isConnected();
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in isConnected :" + e.toString());
			Toast.makeText(this, "Exception in isConnected : "+e.toString(),Toast.LENGTH_LONG).show();
		}
		return false;
	}
	
	public void processFinish(JSONObject output)
	{
		try 
		{
			responseId = output.getLong(RESPONSE_JSON_KEY);
			t.setServerRefId(responseId);
			saveTrip(t);
		} 
		catch (Exception e) 
		{
			Log.i(TAG, "Exception in processFinish :" + e.toString());
			Toast.makeText(this, "Exception in processFinish : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	public void sendToServer()
	{
		try
		{
			JSONObject json = t.toJSON();
			if(isConnected())
			{
				req.execute(json);
			}
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in sendToServer :" + e.toString());
			Toast.makeText(this, "Exception in sendToServer : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	public int dp(int dps)
	{
		final float scale = this.getResources().getDisplayMetrics().density;
		int pixels = (int) (dps * scale + 0.5f);
		return pixels;
	}
	
	public void addPersonToLayout()
	{
		try
		{
			LinearLayout parent = (LinearLayout) findViewById(R.id.friendlayout);
			
			/*************************LINE 1 of Friends Layout****************************/
			LinearLayout child_line1 = new LinearLayout(this);
			LinearLayout.LayoutParams myLayoutParams1 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			myLayoutParams1.topMargin = dp(5);
			child_line1.setId(FRIENDLAYOUTLNONEIDPREFIX + frnd_num);
			child_line1.setOrientation(LinearLayout.HORIZONTAL);
			child_line1.setLayoutParams(myLayoutParams1);
			
			TextView label = new TextView(this);
			LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			labelParams.leftMargin = dp(10);
			label.setLayoutParams(labelParams);
			label.setText("Friend "+frnd_num);
			label.setTextAppearance(this, android.R.style.TextAppearance_Small);
			label.setTypeface(Typeface.MONOSPACE);
			label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			label.setPadding(dp(5), dp(5), dp(5), dp(5));
			  
			EditText name = new EditText(this);
			LayoutParams nameParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			nameParams.leftMargin = dp(10);
			name.setId(FRIENDNAMEIDPREFIX + frnd_num);
			name.setLayoutParams(nameParams);
			name.setHeight(dp(10));
			name.setWidth(dp(100));
			
			EditText loc = new EditText(this);
			LayoutParams locParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			locParams.leftMargin = dp(5);
			loc.setId(FRIENDLOCATIONIDPREFIX + frnd_num);
			loc.setLayoutParams(nameParams);
			loc.setHeight(dp(10));
			loc.setWidth(dp(100));
			
			child_line1.addView(label);
			child_line1.addView(name);
			child_line1.addView(loc);
			/***********************END LINE 1 of Friends Layout**************************/
			
			/*************************LINE 2 of Friends Layout****************************/
			LinearLayout child_line2 = new LinearLayout(this);
			LinearLayout.LayoutParams myLayoutParams2 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			myLayoutParams2.topMargin = dp(5);
			child_line2.setId(FRIENDLAYOUTLNTWOIDPREFIX + frnd_num);
			child_line2.setOrientation(LinearLayout.HORIZONTAL);
			child_line2.setLayoutParams(myLayoutParams2);
			
			TextView phonelabel = new TextView(this);
			LayoutParams phonelabelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			phonelabelParams.leftMargin = dp(40);
			phonelabel.setLayoutParams(phonelabelParams);
			phonelabel.setText("Phone ");
			phonelabel.setTextAppearance(this, android.R.style.TextAppearance_Small);
			phonelabel.setTypeface(Typeface.MONOSPACE);
			phonelabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
			phonelabel.setPadding(dp(5), dp(5), dp(5), dp(5));
			
			EditText phone = new EditText(this);
			LayoutParams phoneParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			phoneParams.leftMargin = dp(10);
			phone.setId(FRIENDPHONEIDPREFIX + frnd_num);
			phone.setLayoutParams(phoneParams);
			phone.setHeight(dp(10));
			phone.setWidth(dp(150));
			
			child_line2.addView(phonelabel);
			child_line2.addView(phone);
			/***********************END LINE 2 of Friends Layout**************************/
			
			parent.addView(child_line1);
			parent.addView(child_line2);
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in addPersonToLayout :" + e.toString());
			Toast.makeText(this, "Exception in addPersonToLayout : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	public void deletePersonFromLayout()
	{
		try
		{
			LinearLayout parent = (LinearLayout) findViewById(R.id.friendlayout);
			
			parent.removeView(findViewById(FRIENDLAYOUTLNONEIDPREFIX + frnd_num));
			parent.removeView(findViewById(FRIENDLAYOUTLNTWOIDPREFIX + frnd_num));
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in deletePersonFromLayout");
			Toast.makeText(this, "Exception in deletePersonFromLayout : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onBackPressed() 
	{
		cancelTripCreation();
	}
	
	public Trip createTrip() 
	{
		try
		{
			EditText tripNameW = (EditText) findViewById(R.id.edittripname);
			String tripName = tripNameW.getText().toString();
			EditText destNameW = (EditText) findViewById(R.id.editdestname);
			String destName = destNameW.getText().toString();
			EditText creatorW = (EditText) findViewById(R.id.editcreatorname);
			String creator = creatorW.getText().toString();
			
			
			ArrayList<Person> friends = new ArrayList<Person>();
			for(int i = 1; i <= frnd_num; i++)
			{
				EditText personName = (EditText) findViewById(FRIENDNAMEIDPREFIX + i);
				String name = personName.getText().toString();
				
				EditText personLocation = (EditText) findViewById(FRIENDLOCATIONIDPREFIX + i);
				String location = personLocation.getText().toString();
				
				EditText personPhone = (EditText) findViewById(FRIENDPHONEIDPREFIX + i);
				String phone = personPhone.getText().toString();
				
				friends.add(new Person(name, location, phone));
			}
			
			if(tripName.trim().isEmpty() || destName.trim().isEmpty() || creator.trim().isEmpty() || friends.isEmpty())
			{
				Toast.makeText(this, "Please enter all fields",Toast.LENGTH_LONG).show();
				return null;
			}
			
			Trip t = new Trip(0, 0, tripName, destName, creator, date, friends);
			return t;
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in createTrip :" + e.toString());
			Toast.makeText(this, "Exception in createTrip : "+e.toString(),Toast.LENGTH_LONG).show();
			return null;
		}
	}
	public boolean validate(String tripName, String meetPoint, String nearBy, String organizerName)
	{
		if(tripName.equals("") || meetPoint.equals("") || nearBy.equals("") || organizerName.equals(""))
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	public boolean saveTrip(Trip trip) 
	{
		String tripName = ((EditText)findViewById(R.id.edittripname)).getText().toString();
		String meetPoint = ((EditText)findViewById(R.id.editdestname)).getText().toString();
		String nearBy = ((EditText)findViewById(R.id.viewnearby)).getText().toString();
		String organizerName = ((EditText)findViewById(R.id.editcreatorname)).getText().toString();
		
		if(!validate(tripName, meetPoint, nearBy, organizerName))
		{
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Blank Fields Encounterred");
			builder.setMessage("Please Enter All the Fields!!").setCancelable(false).
			setNegativeButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return false;
			
		}
		else
		{
		try
		{
			TripDatabaseHelper db = new TripDatabaseHelper(this);
			
			long tripId = db.insertTrip(trip);
			trip.setTripId(tripId);
			for(Person p : trip.getFriends())
			{
				/*long person_id = */db.insertPerson(tripId, p);
			}
			Toast.makeText(this, "Trip Saved ",Toast.LENGTH_LONG).show();
			
			Intent i = new Intent(this, CreateTripActivity.class);
			Bundle b = new Bundle();
			b.putParcelable("trip", trip);
			i.putExtras(b);
			setResult(RESULT_OK, i);
			finish();
			return true;
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in persistTrip :" + e.toString());
			Toast.makeText(this, "Exception in persistTrip : "+e.toString(),Toast.LENGTH_LONG).show();
			return false;
		}
		}
	}

	public void cancelTripCreation() 
	{
		try
		{
			setResult(RESULT_CANCELED, getIntent());
		//	Toast.makeText(this, "Trip Creation Cancelled!",Toast.LENGTH_SHORT).show();
			cancelTrip();
			//finish();
		}
		catch(Exception e)
		{
			Log.i(TAG, "Exception in cancelTripCreation :" + e.toString());
			Toast.makeText(this, "Exception in cancelTripCreation : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) 
	{
		super.onActivityResult(reqCode, resultCode, data);
		String name = null;
		String phone = null;
		try
		{
			if (reqCode == PICK_CONTACT && resultCode == Activity.RESULT_OK)
    		{
    			Uri contactData = data.getData();
    			Cursor c = getContentResolver().query(contactData, null, null, null, null);
    			if (c.moveToFirst())
    			{
    				String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

    				String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

    				if (hasPhone.equalsIgnoreCase("1")) 
    				{
    					Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null, 
                             ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ id,null, null);
    					phones.moveToFirst();
    					phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
    					
    					name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

    					String fullname[] = name.split(" ");
				    	EditText n = (EditText) findViewById(FRIENDNAMEIDPREFIX + frnd_num);
				    	n.setText(fullname[0]);
				    	
				    	EditText p = (EditText) findViewById(FRIENDPHONEIDPREFIX + frnd_num);
				    	p.setText(phone);
    				}
    			}
    		}
			
			if(reqCode == 3)
			{
				EditText meetPoint = (EditText) findViewById(R.id.editdestname);
				ArrayList<String> getLoc = data.getStringArrayListExtra("retVal");
				for(String det : getLoc)
				{
					Log.d(TAG+"Location Details", det);
				}
				meetPoint.setText(getLoc.get(1));
			}
		}	
		catch(Exception e)
		{
			Log.i(TAG, "Exception in onActivityResult :" + e.toString());
			Toast.makeText(this, "Exception in onActivityResult : "+e.toString(),Toast.LENGTH_LONG).show();
		}
	}
	
	public void getFourSqurDataActivity() {
        EditText srchLocNameET = (EditText) findViewById(R.id.editdestname);
        EditText srchLocTypeET = (EditText) findViewById(R.id.viewnearby);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(HW3API_URI));
        intent.putExtra("searchVal", srchLocNameET.getText()+"::"+srchLocTypeET.getText());
        startActivityForResult(intent, 3);       
    }
	
	public void cancelTrip()
	{
		final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle("Cancel Trip");
		alertBuilder.setMessage("You sure you want to cancel?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						finish();
					}
				})
				.setNegativeButton("No",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						// if this button is clicked, just close
						// the dialog box and do nothing
						dialog.cancel();
						//finish();
					}
				})
				
				;
 
				// create alert dialog
				AlertDialog alertDialog = alertBuilder.create();
 
				// show it
				alertDialog.show();
			}				
	
	}

