package com.chattylabs.sdk.android.voice;

import android.annotation.SuppressLint;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

public final class PeripheralHeadsetDevice implements Peripheral.Device {

    private AudioManager audioManager;

    PeripheralHeadsetDevice(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    @Override
    public boolean isConnected() {
        try {
            return audioManager.isWiredHeadsetOn();
        } catch (Exception ignored) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
                for (AudioDeviceInfo deviceInfo : audioDevices) {
                    if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
