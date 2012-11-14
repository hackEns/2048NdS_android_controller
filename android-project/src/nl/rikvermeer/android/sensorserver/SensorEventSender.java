package nl.rikvermeer.android.sensorserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.util.Log;

public class SensorEventSender extends AsyncTask<String, Integer, String> implements SensorEventListener {
	private SensorEventSenderCallbackListener[] listeners;
	private InetAddress targetInetAddress;
	private static DatagramSocket socket;
	private static final int PORT = 5555;
	private static final int CLIENT_PORT = 5555;
	
	public SensorEventSender(SensorEventSenderCallbackListener... listeners) {
		if(listeners != null) {
			this.listeners = listeners;
		}
		//this.targetInetAddress = targetInetAddress;
		try {
			targetInetAddress = InetAddress.getByName("172.30.21.185");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized static DatagramSocket getSocket() {
		if( socket == null ) {
			synchronized( DatagramSocket.class ) {
				try {
					socket = new DatagramSocket(PORT);
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
		DatagramPacket packet = new DatagramPacket(params[0].getBytes(), params[0].length(),
				this.targetInetAddress, CLIENT_PORT);
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
		new SensorEventSender().execute("{ACC_X:" + event.values[0] + "}");
	}
}

interface SensorEventSenderCallbackListener {
	public void onPostExecute(String result);
}