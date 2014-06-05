package nl.rikvermeer.android.sensorserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import nl.rikvermeer.android.sensorserver.R;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;
import android.text.method.ScrollingMovementMethod;
import android.text.format.Time;

public class MainActivity extends FragmentActivity implements SensorEventSenderCallbackListener {

    private static final int RESULT_SETTINGS = 1;
    private SensorManager mSensorManager;
    private Sensor mRotSensor;
    private Sensor mAccelerometer;
    private Sensor mMagnetic;
    private SensorEventSender listener;
    private WakeLock wakeLock;
    private TextView textview;

    private boolean unset = true;

    private float pitch = Float.POSITIVE_INFINITY;
    private float roll = Float.POSITIVE_INFINITY;
    private float yaw = Float.POSITIVE_INFINITY;

    private int upvec = -1;
    private float updir = 0;
    private float thresh = 0.8f;
    private float leftvec[] = new float[3];
    private float fwdvec[] = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fwdvec[0] = 1;
        fwdvec[1] = 0;

        leftvec[0] = 0;
        leftvec[1] = 1;

        super.onCreate(savedInstanceState);
        LinearLayout view = new LinearLayout(this);

        Button calib_btn = new Button(this);
        calib_btn.setText("Calibrate");
        calib_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calibrate();
            }
        });
        view.addView(calib_btn);

        textview = new TextView(this);
        textview.setText("");
        textview.setMovementMethod(new ScrollingMovementMethod());
        textview.setSelected(true);
        view.addView(textview);

        setContentView(view);
        listener = new SensorEventSender(this, this);

        PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorSender Lock");
        wakeLock.acquire();

        if(mSensorManager == null) {
          mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        }
        if(mRotSensor == null) {
          mRotSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        if (mRotSensor == null) {
          if(mAccelerometer == null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
          }
          if(mMagnetic == null) {
            mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
          }
          mSensorManager.registerListener(listener, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
          mSensorManager.registerListener(listener, mMagnetic, SensorManager.SENSOR_DELAY_GAME);
        } else {
          mSensorManager.registerListener(listener, mRotSensor, 10000);
        }
    }

    // Calibration: le device doit être à plat comme posé sur une table avec l'écran visible
    //
    // On sélectionne les vecteurs correspondant à "aller vers le haut/vers
    // l'avant" et "aller vers la gauche"
    private void calibrate() {
        fwdvec[0] = listener.mRotationM[1];
        fwdvec[1] = listener.mRotationM[5];
        leftvec[0] = -listener.mRotationM[0];
        leftvec[1] = -listener.mRotationM[4];
        float fwdnorm = (float) Math.sqrt(fwdvec[0] * fwdvec[0] + fwdvec[1] * fwdvec[1]);
        fwdvec[0] /= fwdnorm;
        fwdvec[1] /= fwdnorm;
        float leftnorm = (float) Math.sqrt(leftvec[0] * leftvec[0] + leftvec[1] * leftvec[1]);
        leftvec[0] /= leftnorm;
        leftvec[1] /= leftnorm;
        addText("Calibration done");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent i = new Intent(this, UserSettingActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSensorManager == null) {
        	mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        }
        mSensorManager.unregisterListener(listener);
        wakeLock.release();
    }

    private void getUpVec() {
      int max_i = 0;
      float max_z = 0;
      for (int i = 0; i < 3; ++i) {
        float z = Math.abs(listener.mRotationM[8+i]);
        if (z > max_z) {
          max_z = z;
          max_i = i;
        }
      }
      upvec = max_i;
      updir = Math.signum(listener.mRotationM[8+max_i]);
      max_i = 0;
      float max_dot = 0;
      for (int i = 0; i < 3; ++i) {
        float dot = fwdvec[0] * listener.mRotationM[0+i] + fwdvec[1] * listener.mRotationM[4+i];
        if (Math.abs(dot) > Math.abs(max_dot)) {
          max_dot = dot;
          max_i = i;
        }
      }

      if (Math.abs(max_dot) > 0.6) {
        float fwddir = Math.signum(max_dot);
        fwdvec[0] = fwddir * listener.mRotationM[0+max_i];
        fwdvec[1] = fwddir * listener.mRotationM[4+max_i];
        float fwdnorm = (float) Math.sqrt(fwdvec[0] * fwdvec[0] + fwdvec[1] * fwdvec[1]);
        fwdvec[0] /= fwdnorm;
        fwdvec[1] /= fwdnorm;

        leftvec[0] = -fwdvec[1];
        leftvec[1] = fwdvec[0];
        addText("Reference updated");
      }
    }

    private void addText(String add) {
      Time now = new Time();
      now.setToNow();
      textview.append(now.format("%H:%M:%S") + ": " + add + "\n");
      textview.post(new Runnable() {
        public void run() {
          Log.d("nds_sensor", textview.getLayout() + " |");
          final int scrollAmount = textview.getLayout().getLineTop(textview.getLineCount()) - textview.getHeight();
          if (scrollAmount > 0)
              textview.scrollTo(0, scrollAmount);
          else
              textview.scrollTo(0, 0);
        }
      });
    }

    @Override
    public void onPostExecute(String result) {
      if (result.length() != 0) return;

      if (unset) {
        unset = false;
        getUpVec();
      } else {
        float vec[] = new float[3];
        vec[0] = updir * listener.mRotationM[0+upvec];
        vec[1] = updir * listener.mRotationM[4+upvec];
        vec[2] = updir * listener.mRotationM[8+upvec];

        /*
        if (upvec != 1) {
          fwdvec[0] = listener.mRotationM[1];
          fwdvec[1] = listener.mRotationM[5];
        } else {
          fwdvec[0] = - updir * listener.mRotationM[0];
          fwdvec[1] = - updir * listener.mRotationM[4];
        }
        if (upvec != 2) {
          leftvec[0] = listener.mRotationM[2];
          leftvec[1] = listener.mRotationM[6];
          if (upvec == 0) {
            leftvec[0] *= updir;
            leftvec[1] *= updir;
          }
        } else {
          leftvec[0] = - updir * listener.mRotationM[0];
          leftvec[1] = - updir * listener.mRotationM[4];
        }
        */

        if (vec[0] * fwdvec[0] + vec[1] * fwdvec[1] < - thresh) {
          addText("DOWN");
          listener.sendUDP("DOWN\n");
        } else if (vec[0] * fwdvec[0] + vec[1] * fwdvec[1] > thresh) {
          addText("UP");
          listener.sendUDP("UP\n");
        } else if (vec[0] * leftvec[0] + vec[1] * leftvec[1] < - thresh) {
          addText("RIGHT");
          listener.sendUDP("RIGHT\n");
        } else if (vec[0] * leftvec[0] + vec[1] * leftvec[1] > thresh) {
          addText("LEFT");
          listener.sendUDP("LEFT\n");
        } else {
          return;
        }
        getUpVec();

        //listener.sendUDP("{"+vec[0]+","+vec[1]+","+vec[2]+"}\n");
      }
    }
}
