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

import com.broadcom.app.ledevicepicker.DevicePicker;
import com.broadcom.app.ledevicepicker.DevicePickerActivity;
import com.broadcom.app.license.LicenseUtils;
import com.broadcom.app.wicedsense.AnimationManager;
import com.broadcom.app.wicedsense.SenseManager;
import com.broadcom.app.wicedsense.Settings;
import com.broadcom.app.wicedsmart.ota.ui.OtaUiHelper;
import com.broadcom.ui.ExitConfirmUtils;

import in.ac.dtu.curo.R;
import in.ac.dtu.curo.fragments.CustomerFragment;

public class CuroMainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, DevicePicker.Callback {

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
        mDevicePickerTitle = getString(R.string.title_devicepicker);
        mDevicePicker = new DevicePicker(this, Settings.PACKAGE_NAME,
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

}
