/*
 *    Copyright (C) 2015 - 2016 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.vrem.wifianalyzer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.vrem.wifianalyzer.navigation.NavigationMenu;
import com.vrem.wifianalyzer.navigation.NavigationMenuView;
import com.vrem.wifianalyzer.settings.Settings;
import com.vrem.wifianalyzer.settings.ThemeStyle;
import com.vrem.wifianalyzer.vendor.model.Database;
import com.vrem.wifianalyzer.vendor.model.VendorService;
import com.vrem.wifianalyzer.wifi.ConnectionView;
import com.vrem.wifianalyzer.wifi.band.WiFiBand;
import com.vrem.wifianalyzer.wifi.band.WiFiChannel;
import com.vrem.wifianalyzer.wifi.scanner.Scanner;
import com.vrem.wifianalyzer.wifi.scanner.Transformer;

import org.apache.commons.lang3.StringUtils;

import static android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;

public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener, OnNavigationItemSelectedListener {
    private static final String WI_FI_ANALYZER_BETA = "WiFi Analyzer BETA";
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_LOCATION = 0x123450;
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_WIFI_STATE = 0x123451;

    private MainContext mainContext = MainContext.INSTANCE;
    private ThemeStyle currentThemeStyle;
    private NavigationMenuView navigationMenuView;
    private boolean subTitle;
    private String currentCountryCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkPermissions();
        initializeMainContext(this);

        Settings settings = mainContext.getSettings();
        settings.initializeDefaultValues();
        setCurrentThemeStyle(settings.getThemeStyle());
        setTheme(getCurrentThemeStyle().themeAppCompatStyle());
        setWiFiChannelPairs();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        settings.registerOnSharedPreferenceChangeListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new WiFiBandToggle());
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationMenuView = new NavigationMenuView(this);
        onNavigationItemSelected(navigationMenuView.defaultMenuItem());

        new ConnectionView(this);
    }

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_CODE_ACCESS_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
        case PERMISSIONS_REQUEST_CODE_ACCESS_LOCATION:
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // permission not granted - exit
                finish();
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initializeMainContext(@NonNull Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Handler handler = new Handler();
        Settings settings = new Settings(context);
        Configuration configuration = getConfiguration(context);

        mainContext.setContext(context);
        mainContext.setConfiguration(configuration);
        mainContext.setResources(context.getResources());
        mainContext.setDatabase(new Database(context));
        mainContext.setSettings(settings);
        mainContext.setVendorService(new VendorService());
        mainContext.setLayoutInflater((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        mainContext.setLogger(new Logger());
        mainContext.setScanner(new Scanner(wifiManager, handler, settings, new Transformer(configuration)));
    }

    private void setWiFiChannelPairs() {
        String countryCode = mainContext.getSettings().getCountryCode();
        if (!countryCode.equals(currentCountryCode)) {
            Pair<WiFiChannel, WiFiChannel> pair = WiFiBand.GHZ5.getWiFiChannels().getWiFiChannelPairFirst(countryCode);
            mainContext.getConfiguration().setWiFiChannelPair(pair);
            currentCountryCode = countryCode;
        }
    }

    private Configuration getConfiguration(@NonNull Context context) {
        boolean isDevelopmentMode = WI_FI_ANALYZER_BETA.equals(context.getString(R.string.app_name));
        return new Configuration(isLargeScreenLayout(), isDevelopmentMode);
    }

    private boolean isLargeScreenLayout() {
        int screenLayoutSize = getResources().getConfiguration().screenLayout & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayoutSize == android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE || screenLayoutSize == android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (shouldReload()) {
            reloadActivity();
        } else {
            setWiFiChannelPairs();
            mainContext.getScanner().update();
            updateSubTitle();
        }
    }

    protected boolean shouldReload() {
        Settings settings = mainContext.getSettings();
        ThemeStyle settingThemeStyle = settings.getThemeStyle();
        boolean result = !getCurrentThemeStyle().equals(settingThemeStyle);
        if (result) {
            setCurrentThemeStyle(settingThemeStyle);
        }
        return result;
    }

    private void reloadActivity() {
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        NavigationMenu item = navigationMenuView.selectedMenuItem(menuItem.getItemId());
        Fragment fragment = item.getFragment();
        if (fragment == null) {
            startActivity(new Intent(this, item.getActivity()));
        } else {
            subTitle = item.isSubTitle();
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, item.getFragment()).commit();
            setTitle(menuItem.getTitle());
            updateSubTitle();
        }
        return true;
    }

    @Override
    protected void onPause() {
        mainContext.getScanner().pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainContext.getScanner().resume();
    }

    private void updateSubTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subTitle ? mainContext.getSettings().getWiFiBand().getBand() : StringUtils.EMPTY);
        }
    }

    public NavigationMenuView getNavigationMenuView() {
        return navigationMenuView;
    }

    private class WiFiBandToggle implements OnClickListener {
        @Override
        public void onClick(View view) {
            mainContext.getSettings().toggleWiFiBand();
        }
    }

    protected ThemeStyle getCurrentThemeStyle() {
        return currentThemeStyle;
    }

    protected void setCurrentThemeStyle(ThemeStyle currentThemeStyle) {
        this.currentThemeStyle = currentThemeStyle;
    }
}
