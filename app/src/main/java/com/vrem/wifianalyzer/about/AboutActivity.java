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

package com.vrem.wifianalyzer.about;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.vrem.wifianalyzer.Configuration;
import com.vrem.wifianalyzer.MainActivity;
import com.vrem.wifianalyzer.MainContext;
import com.vrem.wifianalyzer.R;
import com.vrem.wifianalyzer.settings.Settings;
import com.vrem.wifianalyzer.settings.ThemeStyle;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Settings settings = MainContext.INSTANCE.getSettings();
        ThemeStyle themeStyle = settings.getThemeStyle();
        setTheme(themeStyle.themeAppCompatStyle());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.about_content);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setApplicationName();
        setPackageName();
        setVersionNumber();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setVersionNumber() {
        MainContext mainContext = MainContext.INSTANCE;
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            String versionInfo = packageInfo.versionName;
            Configuration configuration = mainContext.getConfiguration();
            if (configuration.isDevelopmentMode()) {
                versionInfo += " - " + packageInfo.versionCode + " SDK:" + Build.VERSION.SDK_INT;
            }
            ((TextView) findViewById(R.id.about_version_info)).setText(versionInfo);
        } catch (PackageManager.NameNotFoundException e) {
            mainContext.getLogger().error(this, "Version Information", e);
        }
    }

    private void setPackageName() {
        Configuration configuration = MainContext.INSTANCE.getConfiguration();
        if (configuration.isDevelopmentMode()) {
            TextView textView = (TextView) findViewById(R.id.about_package_name);
            textView.setText(getPackageName());
            textView.setVisibility(View.VISIBLE);
        }
    }

    private void setApplicationName() {
        Configuration configuration = MainContext.INSTANCE.getConfiguration();
        if (configuration.isDevelopmentMode()) {
            TextView textView = (TextView) findViewById(R.id.about_app_name);
            textView.setText(textView.getText() + " " + MainActivity.WI_FI_ANALYZER_BETA);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
