package com.chattylabs.demo.voice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.chattylabs.android.commons.PermissionsHelper;
import com.chattylabs.android.commons.ThreadUtils;
import com.chattylabs.sdk.android.voice.AmazonSpeechSynthesizer;
import com.chattylabs.sdk.android.voice.AndroidSpeechRecognizer;
import com.chattylabs.sdk.android.voice.AndroidSpeechSynthesizer;
import com.chattylabs.sdk.android.voice.ConversationalFlowComponent;
import com.chattylabs.sdk.android.voice.GoogleSpeechRecognizer;
import com.chattylabs.sdk.android.voice.GoogleSpeechSynthesizer;
import com.chattylabs.sdk.android.voice.RecognizerListener;
import com.chattylabs.sdk.android.voice.SpeechRecognizerComponent;
import com.chattylabs.sdk.android.voice.SpeechSynthesizerComponent;
import com.chattylabs.sdk.android.voice.SynthesizerListener;
import com.chattylabs.sdk.android.voice.TextFilterForUrl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;

abstract class BaseActivity extends DaggerAppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String ANDROID = "Android";
    private static final String GOOGLE = "Google";
    private static final String AMAZON = "Amazon";

    private String addonType = ANDROID;
    private static LinkedHashMap<Integer, String> addonMap = new LinkedHashMap<>();

    static {
        addonMap.put(0, ANDROID);
        addonMap.put(1, GOOGLE);
        addonMap.put(2, AMAZON);
    }

    @Inject ConversationalFlowComponent component;
    ThreadUtils.SerialThread serialThread;
    SpeechSynthesizerComponent synthesizer;
    SpeechRecognizerComponent recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serialThread = ThreadUtils.newSerialThread();
        setup();
        //UpdateManager.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        //CrashManager.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //UpdateManager.unregister();
        serialThread.shutdownNow();
        component.shutdown();
    }

    void initCommonViews() {
        Spinner addonSpinner = findViewById(R.id.addon);
        if (addonSpinner != null) {
            // Create an ArrayAdapter of the addons
            List<String> addonList = Arrays.asList(addonMap.values().toArray(new String[addonMap.size()]));
            ArrayAdapter<String> addonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, addonList);
            addonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            addonSpinner.setAdapter(addonAdapter);
            addonSpinner.setSelection(0);
            addonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    addonType = addonMap.get(position);
                    setup();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void setup() {
        component.updateConfiguration(builder ->
                builder.setGoogleCredentialsResourceFile(() -> R.raw.credential)
                        .setRecognizerServiceType(() -> {
                            if (GOOGLE.equals(addonType)) {
                                return GoogleSpeechRecognizer.class;
                            } else {
                                return AndroidSpeechRecognizer.class;
                            }
                        })
                        .setSynthesizerServiceType(() -> {
                            if (AMAZON.equals(addonType)) {
                                return AmazonSpeechSynthesizer.class;
                            } else if (GOOGLE.equals(addonType)) {
                                return GoogleSpeechSynthesizer.class;
                            } else {
                                return AndroidSpeechSynthesizer.class;
                            }
                        })
                        .setSpeechLanguage(Locale::getDefault).build());

        String[] perms = component.requiredPermissions();
        PermissionsHelper.check(this,
                perms,
                () -> onRequestPermissionsResult(
                        202, perms,
                        new int[]{PackageManager.PERMISSION_GRANTED}), 202);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 202 &&
                PermissionsHelper.allGranted(grantResults)) {
            serialThread.addTask(() -> {
                component.checkSpeechSynthesizerStatus(this, synthesizerStatus -> {
                    if (synthesizerStatus == SynthesizerListener.Status.AVAILABLE) {
                        synthesizer = component.getSpeechSynthesizer(this);
                        synthesizer.addFilter(new TextFilterForUrl());
                    }
                });
                component.checkSpeechRecognizerStatus(this, recognizerStatus -> {
                    if (recognizerStatus == RecognizerListener.Status.AVAILABLE) {
                        recognizer = component.getSpeechRecognizer(this);
                    }
                });
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.demos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.demo_custom_conversation:
                ContextCompat.startActivity(this,
                        new Intent(this, TestTheComponentActivity.class), null);
                return true;
            case R.id.demo_build_from_json:
                ContextCompat.startActivity(this,
                        new Intent(this, BuildFromJsonActivity.class), null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
