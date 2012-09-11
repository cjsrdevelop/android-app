package com.CJSR;

import com.CJSR.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class mediaService extends Service {
	// Declare variables
	// TAG
	static final String TAG = "mediaService";
	// MediaPlayer
	MediaPlayer mediaPlayer;
	// State Listeners
	private PhoneStateListener phoneStateListener;
	private TelephonyManager telephonyManager;
	// Head set
	private int headsetSwitch = 1;
	// Notification
	private static final int NOTIFICATION_ID = 1;
	// BroadcastReceivers and Intents
	public static final String BROADCAST_BUFFER = "com.CJSR.broadcastbuffer";
	public static final String BROADCAST_ALERT = "com.CJSR.alertbuffer";
	public static final String BROADCAST_PLAYIMAGE = "com.CJSR.broadcastplayimage";
	Intent bufferIntent;
	Intent alertIntent;
	Intent playImageIntent;

	// This method creates, sets the data location, and prepares the
	// mediaPlayer.
	@Override
	public void onCreate() {
		super.onCreate();

		// Log.d(TAG, "onCreate");
		// Instantiate intents
		alertIntent = new Intent(BROADCAST_ALERT);
		bufferIntent = new Intent(BROADCAST_BUFFER);
		playImageIntent = new Intent(BROADCAST_PLAYIMAGE);

		// Instantiate mediaPlayer
		mediaPlayer = new MediaPlayer();

		// Start Telephone Checker
		// Log.d(TAG, "Starting TelephonyManager");
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		// Log.d(TAG, "Starting PhoneStateListener");
		phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				// Log.d(TAG, "Starting CallStateChange");
				switch (state) {
				case TelephonyManager.CALL_STATE_OFFHOOK:
				case TelephonyManager.CALL_STATE_RINGING:
					if (mediaPlayer != null) {
						stopSelf();

					}

					break;
				}

			}
		};

		// This happens when there is an error with mediaPlayer
		mediaPlayer.setOnErrorListener(new OnErrorListener() {
			public boolean onError(MediaPlayer mp, int what, int extra) {
				// Log.d(TAG, "mediaPlayer error");
				sendBufferCompleteBroadcast();
				sendAlert();
				return true;
			}
		});

		// Register the listener with the telephony manager
		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);

		try {

			// Stream Location
			mediaPlayer
					.setDataSource("http://CJSR.streamon.fm:8000/CJSR-32k-m.mp3");
			// Send Buffering broadcast
			sendBufferingBroadcast();
			mediaPlayer.prepareAsync();
		} catch (Exception e) {
			e.printStackTrace();
			// Log.d(TAG, "release mediaPlayer");
			mediaPlayer.release();

		}

		// registering head set receiver
		registerReceiver(headsetReceiver, new IntentFilter(
				Intent.ACTION_HEADSET_PLUG));
	}

	// This method closes the buffering dialog, starts the mediaPlayer, and sets
	// the notification
	@Override
	public void onStart(Intent intent, int startid) {
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
			public void onPrepared(MediaPlayer mediaPlayer) {
				// Log.d(TAG, "mediaPlayer starting");
				sendBufferCompleteBroadcast();
				mediaPlayer.start();
				initNotification();
			}
		});

	}

	// If head phones get unplugged; stop music and service.
	private BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
		private boolean headsetConnected = false;

		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.d(TAG, "ACTION_HEADSET_PLUG Intent received");
			if (intent.hasExtra("state")) {
				if (headsetConnected && intent.getIntExtra("state", 0) == 0) {
					headsetConnected = false;
					headsetSwitch = 0;
					// Log.d(TAG, "State =  Headset disconnected");
				} else if (!headsetConnected
						&& intent.getIntExtra("state", 0) == 1) {
					headsetConnected = true;
					headsetSwitch = 1;
					// Log.d(TAG, "State =  Headset connected");
				}

			}

			switch (headsetSwitch) {
			case (0):
				headsetDisconnected();
				break;
			case (1):
				break;
			}
		}
	};

	private void headsetDisconnected() {
		stopSelf();
	}

	// This method stops and releases the media player.
	@Override
	public void onDestroy() {
		super.onDestroy();
		// Log.d(TAG, "onDestroy");
		mediaPlayer.reset();
		mediaPlayer.release();
		unregisterReceiver(headsetReceiver);
		Log.w(TAG, "playerReceiver has been unregistered");
		cancelNotification();
		setPlayImage();
	}

	// Notification Method (makes sure the service doesn't get killed)
	private void initNotification() {
		// Log.d(TAG, "Notification started");
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = "Happy Listening! Love, CJSR";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		Context context = getApplicationContext();
		CharSequence contentTitle = "CJSR 88.5FM";
		CharSequence contentText = "Listener supported, volunteer powered.";
		Intent notificationIntent = new Intent(this, CJSRActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	// Cancel Notification Method
	private void cancelNotification() {
		// Log.d(TAG, "Closing Notification");
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.cancel(NOTIFICATION_ID);
	}

	// send message to activity about buffering
	private void sendBufferingBroadcast() {
		// Log.d(TAG, "BufferStartedSent");
		bufferIntent.putExtra("buffering", "1");
		sendBroadcast(bufferIntent);
	}

	// send message to activity about completed buffering
	private void sendBufferCompleteBroadcast() {
		// Log.d(TAG, "BufferCompleteSent");
		bufferIntent.putExtra("buffering", "0");
		sendBroadcast(bufferIntent);
	}

	// send message to activity about completed buffering
	private void sendAlert() {
		// Log.d(TAG, "Alert sent");
		sendBroadcast(alertIntent);
	}

	// send intent to set play image button
	private void setPlayImage() {
		// Log.d(TAG, "Play image sent");
		sendBroadcast(playImageIntent);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}