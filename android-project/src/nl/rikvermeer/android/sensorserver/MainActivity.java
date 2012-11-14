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
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private final TextSectionFragment fragment = new TextSectionFragment();
    private SensorEventListener listener;
    public MyNetwork network;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); 

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), fragment);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        listener = new SensorUpdater(fragment, this);
        network = new MyNetwork();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if(mSensorManager == null) {
        	mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        }
        if(mAccelerometer == null) {
        	mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        mSensorManager.registerListener(listener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if(mSensorManager == null) {
        	mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        }
        
        mSensorManager.unregisterListener(listener);
    }
    
    
    class MyNetwork {
    	private static final int PORT = 5555;
        private static final int DISCOVERY_PORT = 5555;
        private DatagramSocket socket;
        private InetAddress broadcastAddress;
        
    	public MyNetwork() {
    		try {
				init();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	private void init() throws IOException {
    		socket = new DatagramSocket(PORT);
        	//socket.setBroadcast(true);
        	getBroadcastAddress();
    	}
    	
    	public byte[] intToByteArray(int value) {
    	    return new byte[] {
    	            (byte)(value >>> 24),
    	            (byte)(value >>> 16),
    	            (byte)(value >>> 8),
    	            (byte)value};
    	}
    	
    	private InetAddress getBroadcastAddress() throws IOException {
    		broadcastAddress = InetAddress.getByName("172.30.21.185");
    		if( broadcastAddress == null ) {
	            WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
	            DhcpInfo dhcp = wifi.getDhcpInfo();
	            // handle null somehow
	
	            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	            byte[] quads = new byte[4];
	            for (int k = 0; k < 4; k++)
	              quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	            this.broadcastAddress = InetAddress.getByAddress(quads);
    		}
    		System.out.println(broadcastAddress.toString());
            return this.broadcastAddress;
        }
    	
    	public void send(String data) throws IOException {
    		if(data != null) {
    			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
    						getBroadcastAddress(), DISCOVERY_PORT);
    			if( packet != null && socket != null) {
    				socket.send(packet);
    			}
    		}
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

    	private Fragment fragment;
        public SectionsPagerAdapter(FragmentManager fm, Fragment fragment) {
            super(fm);
            this.fragment = fragment;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
        	if(position == 0) {
        		Bundle args = new Bundle();
	            args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
	            this.fragment.setArguments(args);
	            return this.fragment;
        	} else {
	            Fragment fragment = new DummySectionFragment();
	            Bundle args = new Bundle();
	            args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
	            fragment.setArguments(args);
	            return fragment;
        	}
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase();
                case 1:
                    return getString(R.string.title_section2).toUpperCase();
                case 2:
                    return getString(R.string.title_section3).toUpperCase();
            }
            return null;
        }
    }
    
    
    
    
    
    public static class SensorUpdater implements SensorEventListener {
    	private TextSectionFragment fragment;
    	private MainActivity parent;
    	
    	public SensorUpdater(TextSectionFragment fragment, MainActivity parent) {
    		this.fragment = fragment;
    		this.parent = parent;
    	}
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void onSensorChanged(SensorEvent event) {
			fragment.update("Values[0] = " + event.values[0]);
			try {
				parent.network.send("{ACC_X:" + event.values[0] + "}");
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Update sensor acc: " + event.values[0]);
		}
    	
    }
    
    public static class TextSectionFragment extends Fragment {
    	public static final String ARG_SECTION_NUMBER = "section_number";
    	private TextView view;
    	public TextSectionFragment() {
    		
    	}
    	@Override
    	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    		this.view = new TextView(getActivity());
            this.view.setGravity(Gravity.CENTER);
            this.view.setText("started");
            return this.view;
    	}
    	
    	public void update(String text) {
    		if(view != null)
    			this.view.setText(text);
    	}
    }

    /**
     * A dummy fragment representing a section of the app, but that simply
     * displays dummy text.
     */
    public static class DummySectionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        public static final String ARG_SECTION_NUMBER = "section_number";

        public DummySectionFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            // Create a new TextView and set its text to the fragment's section
            // number argument value.
            TextView textView = new TextView(getActivity());
            textView.setGravity(Gravity.CENTER);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return textView;
        }
    }

}
