/*
 * WiFi Analyzer
 * Copyright (C) 2016  VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.vrem.wifianalyzer.wifi;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.vrem.wifianalyzer.R;
import com.vrem.wifianalyzer.wifi.model.Security;
import com.vrem.wifianalyzer.wifi.model.Strength;
import com.vrem.wifianalyzer.wifi.model.WiFiConnection;
import com.vrem.wifianalyzer.wifi.model.WiFiDetail;
import com.vrem.wifianalyzer.wifi.model.WiFiSignal;

import org.apache.commons.lang3.StringUtils;

public class AccessPointsDetail {
    public void setView(@NonNull Resources resources, @NonNull View view, @NonNull WiFiDetail wiFiDetail, boolean child, boolean frequencyRange) {
        ((TextView) view.findViewById(R.id.ssid)).setText(wiFiDetail.getTitle());

        TextView textIPAddress = (TextView) view.findViewById(R.id.ipAddress);
        TextView textLinkSpeed = (TextView) view.findViewById(R.id.linkSpeed);
        String ipAddress = wiFiDetail.getWiFiAdditional().getIPAddress();
        if (StringUtils.isBlank(ipAddress)) {
            textIPAddress.setVisibility(View.GONE);
            textLinkSpeed.setVisibility(View.GONE);
        } else {
            textIPAddress.setVisibility(View.VISIBLE);
            textIPAddress.setText(ipAddress);

            int linkSpeed = wiFiDetail.getWiFiAdditional().getLinkSpeed();
            if (linkSpeed == WiFiConnection.LINK_SPEED_INVALID) {
                textLinkSpeed.setVisibility(View.GONE);
            } else {
                textLinkSpeed.setVisibility(View.VISIBLE);
                textLinkSpeed.setText(String.format("%d%s", linkSpeed, WifiInfo.LINK_SPEED_UNITS));
            }
        }

        ImageView configuredImage = (ImageView) view.findViewById(R.id.configuredImage);
        if (wiFiDetail.getWiFiAdditional().isConfiguredNetwork()) {
            configuredImage.setVisibility(View.VISIBLE);
            configuredImage.setColorFilter(resources.getColor(R.color.connected));
        } else {
            configuredImage.setVisibility(View.GONE);
        }

        WiFiSignal wiFiSignal = wiFiDetail.getWiFiSignal();
        Strength strength = wiFiSignal.getStrength();
        ImageView imageView = (ImageView) view.findViewById(R.id.levelImage);
        imageView.setImageResource(strength.imageResource());
        imageView.setColorFilter(resources.getColor(strength.colorResource()));

        Security security = wiFiDetail.getSecurity();
        ImageView securityImage = (ImageView) view.findViewById(R.id.securityImage);
        securityImage.setImageResource(security.imageResource());
        securityImage.setColorFilter(resources.getColor(R.color.icons_color));

        TextView textLevel = (TextView) view.findViewById(R.id.level);
        textLevel.setText(String.format("%ddBm", wiFiSignal.getLevel()));
        textLevel.setTextColor(resources.getColor(strength.colorResource()));

        ((TextView) view.findViewById(R.id.channel)).setText(String.format("%d", wiFiSignal.getWiFiChannel().getChannel()));
        ((TextView) view.findViewById(R.id.frequency)).setText(String.format("%d%s", wiFiSignal.getFrequency(), WifiInfo.FREQUENCY_UNITS));
        ((TextView) view.findViewById(R.id.width)).setText(String.format("(%d%s)", wiFiSignal.getWiFiWidth().getFrequencyWidth(), WifiInfo.FREQUENCY_UNITS));
        ((TextView) view.findViewById(R.id.distance)).setText(String.format("%.1fm", wiFiSignal.getDistance()));
        ((TextView) view.findViewById(R.id.capabilities)).setText(wiFiDetail.getCapabilities());

        TextView textVendor = ((TextView) view.findViewById(R.id.vendor));
        String vendor = wiFiDetail.getWiFiAdditional().getVendorName();
        if (StringUtils.isBlank(vendor)) {
            textVendor.setVisibility(View.GONE);
        } else {
            textVendor.setVisibility(View.VISIBLE);
            textVendor.setText(vendor);
        }

        if (child) {
            view.findViewById(R.id.tab).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.tab).setVisibility(View.GONE);
        }

        if (frequencyRange) {
            view.findViewById(R.id.channel_frequency_range_row).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.channel_frequency_range)).setText(String.format("%d - %d %s",
                    wiFiSignal.getFrequencyStart(), wiFiSignal.getFrequencyEnd(), WifiInfo.FREQUENCY_UNITS));
        } else {
            view.findViewById(R.id.channel_frequency_range_row).setVisibility(View.GONE);
        }
    }

    public Dialog popupDialog(@NonNull Context context, @NonNull LayoutInflater inflater, @NonNull WiFiDetail wiFiDetail) {
        View view = inflater.inflate(R.layout.access_points_details_popup, null);
        Dialog dialog = new Dialog(context);
        dialog.setContentView(view);
        setView(context.getResources(), view, wiFiDetail, false, true);
        dialog.findViewById(R.id.popupButton).setOnClickListener(new PopupDialogListener(dialog));
        return dialog;
    }

    private class PopupDialogListener implements OnClickListener {
        private final Dialog dialog;

        PopupDialogListener(@NonNull Dialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View view) {
            dialog.dismiss();
        }
    }

}
