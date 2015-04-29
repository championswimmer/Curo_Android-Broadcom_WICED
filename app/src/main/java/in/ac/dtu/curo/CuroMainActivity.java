package in.ac.dtu.curo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.broadcom.app.ledevicepicker.DevicePicker;
import com.broadcom.app.ledevicepicker.DevicePickerActivity;
import com.broadcom.app.license.LicenseUtils;
import com.broadcom.app.wicedsense.AnimationManager;
import com.broadcom.app.wicedsense.SenseManager;
import com.broadcom.app.wicedsense.SensorDataParser;
import com.broadcom.app.wicedsense.Settings;
import com.broadcom.app.wicedsmart.ota.ui.OtaUiHelper;
import com.broadcom.ui.BluetoothEnabler;
import com.broadcom.ui.ExitConfirmUtils;

import in.ac.dtu.curo.R;
import in.ac.dtu.curo.fragments.CustomerFragment;

public class CuroMainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, DevicePicker.Callback {

    private static final int COMPLETE_INIT = 800;
    private static final int PROCESS_SENSOR_DATA_ON_UI = 801;
    private static final int PROCESS_BATTERY_STATUS_UI = 802;
    private static final int PROCESS_EVENT_DEVICE_UNSUPPORTED = 803;
    private static final int PROCESS_CONNECTION_STATE_CHANGE_UI = 804;
    private static final String FRAGMENT_TEMP = "fragment_temp";



    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private DevicePicker mDevicePicker;
    private String mDevicePickerTitle;
    private int mLastBatteryStatus = -1;
    private boolean mConnectDisconnectPending;
    private SenseManager mSenseManager;
    private Handler mUiHandler;
    private final BluetoothStateReceiver mBtStateReceiver = new BluetoothStateReceiver();
    private LicenseUtils mLicense;
    private ExitConfirmUtils mExitConfirm;
    private int mInitState;
    private Handler mSensorDataEventHandler;
    private HandlerThread mSensorDataEventThread;
    private final AnimationManager mAnimation = new AnimationManager(
            Settings.ANIMATION_FRAME_DELAY_MS, Settings.ANIMATE_TIME_INTERVAL_MS);
    private final AnimationManager mAnimationSlower = new AnimationManager(
            Settings.ANIMATION_FRAME_DELAY_MS, Settings.ANIMATE_TIME_INTERVAL_MS);
    private final OtaUiHelper mOtaUiHelper = new OtaUiHelper();
    private boolean mShowAppInfoDialog;
    private boolean mFirmwareUpdateCheckPending;
    private boolean mCanAskForFirmwareUpdate;
    private boolean mMandatoryUpdateRequired;
    private boolean mIsTempScaleF = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curo_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        mUiHandler = new Handler(new UiHandlerCallback());

        initDevicePicker();
        registerReceiver(mBtStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fm = getSupportFragmentManager();
        switch (position) {
            case 0:
            default:
                fm.beginTransaction().add(R.id.container, CustomerFragment.newInstance("A", "a")).commit();
                break;
            case 1:
                break;
            case 2:
                break;
        }
    }

    private void initDevicePicker() {
        Log.d("CURO", "initDevicePicker");
        mDevicePickerTitle = getString(R.string.title_devicepicker);
        mDevicePicker = new DevicePicker(this, getPackageName(),
                DevicePickerActivity.class.getName(), this,
                Uri.parse("content://com.brodcom.app.wicedsense/device/pick"));
        mDevicePicker.init();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = "CUSTOMER MODE";
                break;
            case 2:
                mTitle = "MANAGER MODE";
                break;
            case 3:
                mTitle = "STAFF MODE";
                break;
        }
    }


    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.curo_main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_connect) {
            doConnectDisconnetBakchodi();
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void doConnectDisconnetBakchodi() {
        if (!mSenseManager.isConnectedAndAvailable()) {
            if (!mSenseManager.connect()) {
                //updateConnectionStateWidgets();
            }
        } else {
            if (!mSenseManager.disconnect()) {
                //updateConnectionStateWidgets();
            }
        }
    }

    @Override
    public void onDevicePicked(BluetoothDevice device) {
        if (Settings.CHECK_FOR_UPDATES_ON_CONNECT) {
            mCanAskForFirmwareUpdate = true;
        } else {
            mCanAskForFirmwareUpdate = false;
        }
        mSenseManager.setDevice(device);
    }

    @Override
    public void onDevicePickCancelled() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        initResourcesAndResume();
    }

    /**
     * Initialize async resources in series
     *
     * @return
     */
    private boolean initResourcesAndResume() {
        switch (mInitState) {
            case 0:
                mInitState++;
            case 1:
                // Check if BT is on, If not, prompt user
                if (!BluetoothEnabler.checkBluetoothOn(this)) {
                    return false;
                }
                mInitState++;
                SenseManager.init(this);
            case 2:
                // Check if sense manager initialized. If not, keep waiting
                if (waitForSenseManager()) {
                    return false;
                }
                mInitState = -1;
                checkDevicePicked();
        }
        mSenseManager.registerEventCallbackHandler(mSensorDataEventHandler);

        if (mSenseManager.isConnectedAndAvailable()) {
            mSenseManager.enableNotifications(true);
        }
        //Settings.addChangeListener(this);
        return true;
    }

    private class UiHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {

                // These events run on the mUiHandler on the UI Main Thread
                case COMPLETE_INIT:
                    initResourcesAndResume();
                    break;
                case PROCESS_EVENT_DEVICE_UNSUPPORTED:
                    Toast.makeText(getApplicationContext(), R.string.error_unsupported_device,
                            Toast.LENGTH_SHORT).show();
                    break;
                case PROCESS_CONNECTION_STATE_CHANGE_UI:
                    //updateConnectionStateWidgets();
                    break;
                case PROCESS_BATTERY_STATUS_UI:
                    //updateBatteryLevelWidget(msg.arg1);
                    break;
                case PROCESS_SENSOR_DATA_ON_UI:
                    //processSensorData((byte[]) msg.obj);
                    break;
            }
            return true;
        }
    }

    /**
     * Acquire reference to the SenseManager serivce....This is asynchronous
     *
     * @return
     */
    private boolean waitForSenseManager() {
        // Check if the SenseManager is available. If not, keep retrying
        mSenseManager = SenseManager.getInstance();
        if (mSenseManager == null) {
            mUiHandler.sendEmptyMessageDelayed(COMPLETE_INIT, Settings.SERVICE_INIT_TIMEOUT_MS);
            return true;
        }
        return false;
    }

    /**
     * Check if a device has been picked, and launch the device picker if not...
     *
     * @return
     */
    private boolean checkDevicePicked() {
        if (mSenseManager != null && mSenseManager.getDevice() != null) {
            return true;
        }
        // Launch device picker
        launchDevicePicker();
        return false;
    }

    /**
     * Launch the device picker
     */
    private void launchDevicePicker() {
        mDevicePicker.launch(mDevicePickerTitle, null, null);
    }



    /**
     * Handles Bluetooth on/off events. If Bluetooth is turned off, exit this
     * app
     *
     * @author Fred Chen
     *
     */
    private class BluetoothStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            mSensorDataEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (btState) {

                        case BluetoothAdapter.STATE_TURNING_OFF:
                            exitApp();
                            break;
                    }
                }
            });
        }
    }
    /**
     * Exit the application and cleanup resources
     */
    protected void exitApp() {

        SenseManager.destroy();
        finish();
    }

    private long mLastRefreshTimeMs;
    private long mLastRefreshSlowerTimeMs;

    public interface OnValueReturned {
        void returnValue(String instrument, int[] values);
    }


    /**
     * Parses the sensor data bytes and updates the corresponding sensor(s) UI
     * component
     *
     * @param sensorData
     */
    public void processSensorData(byte[] sensorData, OnValueReturned ovr) {
        if (mAnimation != null && mAnimation.useAnimation()) {
            mAnimation.init();
        }

        if (mAnimationSlower != null && mAnimationSlower.useAnimation()) {
            mAnimationSlower.init();
        }

        int maskField = sensorData[0];
        int offset = 0;
        int[] values = new int[3];
        boolean updateView = false;
        long currentTimeMs = System.currentTimeMillis();
        switch (sensorData.length) {
            case 19:
                Log.d("CURO", "processSensorData 19");
                if (currentTimeMs - mLastRefreshTimeMs < Settings.REFRESH_INTERVAL_MS) {
                    return;
                } else {
                    mLastRefreshTimeMs = currentTimeMs;
                }

                // packet type specifying accelerometer, gyro, magno
                offset = 1;
                if (true) {
                    SensorDataParser.getAccelorometerData(sensorData, offset, values);
                    ovr.returnValue("ACCEL", new int[]{values[0], values[1], values[2]});
                    //mAccelerometerFrag.setValue(mAnimation, values[0], values[1], values[2]);
                    updateView = true;
                    offset += SensorDataParser.SENSOR_ACCEL_DATA_SIZE;
                }

                if (true) {
                    SensorDataParser.getGyroData(sensorData, offset, values);
                    ovr.returnValue("GYRO", new int[]{values[0], values[1], values[2]});
                    //mGyroFrag.setValue(mAnimation, values[0], values[1], values[2]);
                    updateView = true;
                    offset += SensorDataParser.SENSOR_GYRO_DATA_SIZE;
                }

                if (true) {
                    SensorDataParser.getMagnometerData(sensorData, offset, values);
                    ovr.returnValue("MAGNET", new int[]{values[0], values[1], values[2]});
                    float angle = SensorDataParser.getCompassAngleDegrees(values);
                    //mCompassFrag.setValue(mAnimation, angle, values[0], values[1], values[2]);
                    updateView = true;
                    offset += SensorDataParser.SENSOR_MAGNO_DATA_SIZE;
                }

                if (updateView && mAnimation != null) {
                    mAnimation.animate();
                }
                break;
            case 7:
                Log.d("CURO", "processSensorData 7");

                if (currentTimeMs - mLastRefreshSlowerTimeMs < Settings.REFRESH_INTERVAL_SLOWER_MS) {
                    return;
                } else {
                    mLastRefreshSlowerTimeMs = currentTimeMs;
                }

                // packet type specifying temp, humid, press
                offset = 1;
                float value = 0;
                if (SensorDataParser.humidityHasChanged(maskField)) {
                    value = SensorDataParser.getHumidityPercent(sensorData, offset);
                    offset += SensorDataParser.SENSOR_HUMD_DATA_SIZE;
                    //mHumidityFrag.setValue(mAnimationSlower, value);
                    updateView = true;
                }
                if (SensorDataParser.pressureHasChanged(maskField)) {
                    value = SensorDataParser.getPressureMBar(sensorData, offset);
                    offset += SensorDataParser.SENSOR_PRES_DATA_SIZE;
                    //mPressureFrag.setValue(mAnimationSlower, value);
                    updateView = true;
                }

                if (SensorDataParser.temperatureHasChanged(maskField)) {
                    if (mIsTempScaleF) {
                        value = SensorDataParser.getTemperatureF(sensorData, offset);
                    } else {
                        value = SensorDataParser.getTemperatureC(sensorData, offset);
                    }
                    offset += SensorDataParser.SENSOR_TEMP_DATA_SIZE;
                    //mTemperatureFrag.setValue(mAnimationSlower, value);
                    updateView = true;
                }
                if (updateView && mAnimationSlower != null) {
                    mAnimationSlower.animate();
                }
                break;
        }

        // If animation is enabled, call animate...
    }

}
