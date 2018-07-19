package com.chattylabs.sdk.android.voice;

import android.app.Application;
import android.content.IntentFilter;
import android.media.AudioManager;

import com.chattylabs.sdk.android.common.Tag;
import com.chattylabs.sdk.android.common.internal.ILogger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BluetoothSco {
    private static final String TAG = Tag.make("BluetoothSco");

    // Lock
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    // States
    private boolean isScoReceiverRegistered; // released
    private boolean isBluetoothScoOn;

    // Resources
    private Application application;
    private AndroidAudioHandler audioHandler;
    private BluetoothScoReceiver bluetoothScoReceiver = new BluetoothScoReceiver();

    // Log stuff
    private ILogger logger;

    public BluetoothSco(Application application, AndroidAudioHandler audioHandler, ILogger logger) {
        this.application = application;
        this.audioHandler = audioHandler;
        this.logger = logger;
        bluetoothScoReceiver = new BluetoothScoReceiver(logger);
    }

    private void registerReceiver(BluetoothScoListener bluetoothScoListener) {
        BluetoothScoListener helper = new BluetoothScoListener() {
            @Override
            public void onConnected() {
                bluetoothScoListener.onConnected();
            }

            @Override
            public void onDisconnected() {
                unregisterReceiver();
                bluetoothScoListener.onDisconnected();
                lock.lock();
                try {
                    condition.signalAll();
                } catch (Exception e) {
                    logger.logException(e);
                } finally {
                    lock.unlock();
                }
            }
        };
        bluetoothScoReceiver.setListener(helper);
        if (!isScoReceiverRegistered) {
            logger.v(TAG, "register sco receiver");
            IntentFilter scoFilter = new IntentFilter();
            scoFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
            application.registerReceiver(bluetoothScoReceiver, scoFilter);
            isScoReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        if (isScoReceiverRegistered) {
            logger.v(TAG, "unregister sco receiver");
            application.unregisterReceiver(bluetoothScoReceiver);
            isScoReceiverRegistered = false;
        }
    }

    public void startSco(BluetoothScoListener bluetoothScoListener) {
        registerReceiver(bluetoothScoListener);
        if (audioHandler.isBluetoothScoAvailableOffCall() && !isBluetoothScoOn) {
            isBluetoothScoOn = true;
            audioHandler.setBluetoothScoOn(true);
            audioHandler.startBluetoothSco();
            logger.v(TAG, "start bluetooth sco");
        }
    }

    public void stopSco() {
        if (audioHandler.isBluetoothScoAvailableOffCall() && isBluetoothScoOn) {
            isBluetoothScoOn = false;
            audioHandler.setBluetoothScoOn(false);
            audioHandler.stopBluetoothSco();
            lock.lock();
            try {
                condition.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.logException(e);
            } finally {
                lock.unlock();
            }
            unregisterReceiver();
            logger.v(TAG, "stop bluetooth sco");
        }
    }

    public boolean isBluetoothScoOn() {
        return isBluetoothScoOn;
    }
}