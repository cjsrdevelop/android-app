package com.CJSR;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;

import com.CJSR.R;

public class CJSRActivity extends Activity {
	// Variables
	// TAG
	// static final String TAG = "CJSRActivity";
	// Dialogs
	private boolean alertDialogOn;
	private static final int DIALOG_ALERT = 0;
	private boolean bufferDialogOn;
	private ProgressDialog pdBuff = null;
	// States
	private boolean isNetworkOn;
	private boolean isMusicOn;
	// Button
	private Button buttonPlayStop;
	// BroadcastReceivers states
	boolean mBufferBroadcastIsRegistered;
	boolean mAlertBroadcastIsRegistered;
	boolean mPlayImageBroadcastIsRegistered;

	// This Method creates all of the on screen elements
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Log.d(TAG, "set view");
		setContentView(R.layout.main);
		// Log.d(TAG, "listener");
		setListeners();
		// Log.d(TAG, "init");
		initViews();
	}

	// This method sets the listner for the buttonPlayStop
	private void setListeners() {
		buttonPlayStop = (Button) findViewById(R.id.buttonPlayStop);
		buttonPlayStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonPlayStopClick();

			}
		});

	}

	// This method controls what happens on a buttonPlayStop click
	private void buttonPlayStopClick() {
		if (!isMusicOn) {
			// Log.d(TAG, "set button stop");
			setStopImage();
			// Log.d(TAG, "start mediaService");
			start();
			// Log.d(TAG, "set isMusicOn true");
			isMusicOn = true;
		} else {
			if (isMusicOn) {
				// Log.d(TAG, "set button play");
				setPlayImage();
				// Log.d(TAG, "stop mediaService");
				stop();
				// Log.d(TAG, "set isMusicon false");
				isMusicOn = false;

			}
		}
	}

	// This method initialises buttonPlayStop and sets the right image
	private void initViews() {
		if (!isMediaServiceOn()) {
			// Log.d(TAG, "set button play");
			setPlayImage();
		} else {
			// Log.d(TAG, "set button stop");
			setStopImage();
		}
	}

	// Method for start button
	private void start() {
		checkConnectivity();
		if (isNetworkOn) {
			// Log.d(TAG, "Starting Service");
			startService(new Intent(this, mediaService.class));

		} else {
			// Log.d(TAG, "Service not started (No Network)-Alert Dialog sent");
			showDialog(DIALOG_ALERT);
			alertDialogOn = true;
		}
	}

	// TODO -see if i can make this better Method for stop button
	private void stop() {
		checkConnectivity();
		if (isNetworkOn) {
			// Log.d(TAG, "Service stopping");
			stopService(new Intent(this, mediaService.class));
		} else if (!isNetworkOn) {
			
		}
	}

	// Method for progress dialog
	private void showPD(Intent bufferIntent) {
		String bufferValue = bufferIntent.getStringExtra("buffering");
		int bufferIntValue = Integer.parseInt(bufferValue);
		// When the broadcasted "buffering" value is 1, show "Buffering"
		// progress dialogue.
		// When the broadcasted "buffering" value is 0, dismiss the progress
		// dialogue.
		switch (bufferIntValue) {
		case 0:
			// Log.v(TAG, "BufferIntValue=0 RemoveBufferDialogue");
			// txtBuffer.setText("");
			if (pdBuff != null) {
				// Log.d(TAG, "Dismiss Dialog");
				pdBuff.dismiss();
			}
			break;

		case 1:
			// Log.d(TAG, "Show Buffer Dialog");
			BufferDialogue();
			break;
		}
	}

	// Broadcast receiver Buffer
	private BroadcastReceiver broadcastBufferReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent bufferIntent) {
			showPD(bufferIntent);
		}
	};

	// Alert reciever Buffer
	private BroadcastReceiver broadcastAlertReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent Intent) {
			// Log.d(TAG, "Show Alert Dialog");
			showDialog(DIALOG_ALERT);
			alertDialogOn = true;
		}
	};

	// Set Stop Image Broadcast Reciever
	private BroadcastReceiver broadcastPlayImageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent Intent) {
			setPlayImage();
		}
	};

	protected void setPlayImage() {
		buttonPlayStop.setBackgroundResource(R.drawable.button_play);

	}

	protected void setStopImage() {
		buttonPlayStop.setBackgroundResource(R.drawable.button_stop);
	}

	// This method opens the CJSR web site
	public void buttonWeb(View v) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("http://www.cjsr.com"));
		startActivity(browserIntent);
	}

	// This method opens a share view
	public void buttonShare(View v) {
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		String shareMessage = "I'm listening to CJSR 88.5FM using the new Android App!";
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);
		startActivity(Intent.createChooser(shareIntent, "Share via"));
	}

	// Method to check connectivity
	private void checkConnectivity() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		if (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.isConnectedOrConnecting()
				|| cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
						.isConnectedOrConnecting())
			isNetworkOn = true;
		else
			isNetworkOn = false;
	}

	@Override
	// unregister reciever
	protected void onPause() {
		unregisterRecievers();
		dismissDialogs();
		super.onPause();
	}

	@Override
	// register recievers
	protected void onResume() {
		registerRecievers();
		// Checks for connectivity onresume displays dialog if not
		checkConnectivity();
		if (!isNetworkOn) {
			// Alert();
			stopService(new Intent(this, mediaService.class));
			showDialog(DIALOG_ALERT);
			alertDialogOn = true;
		}

		super.onResume();
	}

	// check if service is running
	private boolean isMediaServiceOn() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.CJSR.mediaService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	// Alert dialog
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ALERT:
			return new AlertDialog.Builder(CJSRActivity.this)
					.setIconAttribute(R.drawable.ic_launcher)
					.setTitle(R.string.Title)
					.setMessage(R.string.Message)
					.setCancelable(false)
					.setPositiveButton(R.string.positiveButton,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
									buttonPlayStop
											.setBackgroundResource(R.drawable.button_play);
								}

							}).create();
		}
		return null;

	}

	// Progress dialogue...
	private Dialog BufferDialogue() {

		pdBuff = ProgressDialog.show(CJSRActivity.this, null, "Buffering...",
				true);
		bufferDialogOn = true;
		return pdBuff;
	}

	// unregister recievers
	private void unregisterRecievers() {
		if (mBufferBroadcastIsRegistered) {
			unregisterReceiver(broadcastBufferReceiver);
			mBufferBroadcastIsRegistered = false;
		}
		if (mAlertBroadcastIsRegistered) {
			unregisterReceiver(broadcastAlertReceiver);
			mAlertBroadcastIsRegistered = false;
		}
		if (mPlayImageBroadcastIsRegistered) {
			unregisterReceiver(broadcastPlayImageReceiver);
			mPlayImageBroadcastIsRegistered = false;
		}
	}

	private void registerRecievers() {
		if (!mBufferBroadcastIsRegistered) {
			registerReceiver(broadcastBufferReceiver, new IntentFilter(
					mediaService.BROADCAST_BUFFER));
			mBufferBroadcastIsRegistered = true;
		}
		if (!mAlertBroadcastIsRegistered) {
			registerReceiver(broadcastAlertReceiver, new IntentFilter(
					mediaService.BROADCAST_ALERT));
			mAlertBroadcastIsRegistered = true;
		}
		if (!mPlayImageBroadcastIsRegistered) {
			registerReceiver(broadcastPlayImageReceiver, new IntentFilter(
					mediaService.BROADCAST_PLAYIMAGE));
			mPlayImageBroadcastIsRegistered = true;
		}
	}

	// Method to dismiss dialogs
	private void dismissDialogs() {
		if (alertDialogOn) {
			dismissDialog(DIALOG_ALERT);
			alertDialogOn = false;

		} else if (bufferDialogOn) {
			if (pdBuff != null) {
				pdBuff.dismiss();
			}
		}

	}
}