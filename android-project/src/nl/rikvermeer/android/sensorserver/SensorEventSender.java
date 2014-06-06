package nl.rikvermeer.android.sensorserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.util.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.app.Activity;
import android.view.Surface;

public class SensorEventSender extends AsyncTask<String, Integer, String> implements SensorEventListener {
  private static SharedPreferences prefs;
  private Context context;
	private SensorEventSenderCallbackListener[] listeners;
	private InetAddress targetInetAddress = null;
  private String hostname;
	private int port;
	private static final int SOURCE_PORT = 5555;
	private static DatagramSocket socket;

  private boolean gravOk = false;
  private boolean magOk = false;
  public float[] mGravs = new float[3];
  private float[] mGeoMags = new float[3];
  public float[] mRotationM = new float[16];
	
	public SensorEventSender(Context context, SensorEventSenderCallbackListener... listeners) {
		if(listeners != null) {
			this.listeners = listeners;
		}
    if (context != null) {
      this.context = context;
    }
		//this.targetInetAddress = targetInetAddress;
    prefs = PreferenceManager.getDefaultSharedPreferences(this.context);

    hostname = prefs.getString("host", "192.168.1.13");
    port = Integer.valueOf(prefs.getString("port", "5555"));
	}
	
	public synchronized static DatagramSocket getSocket() {
		if( socket == null ) {
			synchronized( DatagramSocket.class ) {
				try {
					socket = new DatagramSocket(SOURCE_PORT);
					Log.d(SensorEventSender.class.toString(), "Created socket: " + socket.toString());
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
		}
		return socket;
	}

	@Override
	protected synchronized String doInBackground(String... params) {
    // Ignore empty messages
    if (params[0].length() == 0)
      return params[0];
    Log.d("nds_sensor_network", params[0]);
		if (this.targetInetAddress == null) {
      try {
        targetInetAddress = InetAddress.getByName(hostname);
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
    }
		DatagramPacket packet = new DatagramPacket(params[0].getBytes(), params[0].length(),
				this.targetInetAddress, this.port);
		try {
			SensorEventSender.getSocket().send(packet);
			Log.d(SensorEventSender.class.toString(), "Send: " + params[0]);
			return params[0];
		} catch (IOException e) {
			e.printStackTrace();
			return "error";
		}
	}

	@Override
    protected void onProgressUpdate(Integer... values) { //
      super.onProgressUpdate(values);
    }

    // Called once the background activity has completed
    @Override
    protected void onPostExecute(String result) { //
      for(SensorEventSenderCallbackListener listener: listeners) {
    	  listener.onPostExecute(result);
      }
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//TODO: implement over TCP
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
    switch (event.sensor.getType()) {
        case Sensor.TYPE_ROTATION_VECTOR:
                SensorManager.getRotationMatrixFromVector(mRotationM, event.values);
                new SensorEventSender(this.context, listeners).execute("");
                //Log.d("plop", "using RV");
                return;
        case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mGravs, 0, 3);
                gravOk = true;
                break;
        case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mGeoMags, 0, 3);
                magOk = true;
                break;
        default:
                return;
    }
    if (magOk && gravOk) {
        SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags);
        new SensorEventSender(this.context, listeners).execute("");
    }
	}

  public void sendUDP(String message) {
      new SensorEventSender(this.context, listeners).execute(message);
  }
}

interface SensorEventSenderCallbackListener {
	public void onPostExecute(String result);
}
