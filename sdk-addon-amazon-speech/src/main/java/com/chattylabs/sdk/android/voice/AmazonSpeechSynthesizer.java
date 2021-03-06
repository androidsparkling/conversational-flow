package com.chattylabs.sdk.android.voice;

import android.app.Application;
import android.media.MediaPlayer;
import android.os.ConditionVariable;
import android.support.annotation.NonNull;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.LanguageCode;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.chattylabs.android.commons.HtmlUtils;
import com.chattylabs.android.commons.Tag;
import com.chattylabs.android.commons.internal.ILogger;
import com.chattylabs.sdk.android.voice.addon.amazon.R;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class AmazonSpeechSynthesizer extends BaseSpeechSynthesizer {

    private static final String TAG = Tag.make("AmazonSpeechSynthesizer");
    private static final String LOG_LABEL = "AMAZON TTS";

    private final Application mApplication;
    private final LanguageCode mLanguageCode;
    private MediaPlayer mMediaPlayer;
    private AmazonPollyPresigningClient mAmazonSpeechClient;
    private Voice mDefaultVoices;

    private final ConditionVariable mCondVar = new ConditionVariable();

    AmazonSpeechSynthesizer(Application application,
                            ComponentConfig configuration,
                            AndroidAudioManager audioManager,
                            BluetoothSco bluetoothSco,
                            ILogger logger) {
        super(configuration, audioManager, bluetoothSco, logger);
        mApplication = application;
        mLanguageCode = LanguageUtil.getDeviceLanguageCode(configuration.getSpeechLanguage());
    }

    @Override
    String getTag() {
        return TAG;
    }

    @Override
    void prepare(SynthesizerListener.OnPrepared onSynthesizerPrepared) {
        if (isTtsNull()) {
            final AWSConfiguration configuration = AWSConfiguration.getConfiguration(mApplication.getResources()
                    .openRawResource(R.raw.awsconfiguration));

            final CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    mApplication.getApplicationContext(),
                    configuration.getPoolId(),
                    Regions.fromName(configuration.getRegion())
            );

            setSynthesizerUtteranceListener(createUtterancesListener());
            mAmazonSpeechClient = new AmazonPollyPresigningClient(credentialsProvider);
            onSynthesizerPrepared.execute(SynthesizerListener.Status.SUCCESS);
        } else {
            onSynthesizerPrepared.execute(SynthesizerListener.Status.SUCCESS);
        }
    }

    private SynthesizerUtteranceListener createUtterancesListener() {
        return new BaseSynthesizerUtteranceListener(this,
                BaseSynthesizerUtteranceListener.Mode.DELEGATE) {
            @Override
            String getTtsLogLabel() {
                return LOG_LABEL;
            }
        };
    }

    @Override
    void executeOnTtsReady(String utteranceId, String text, HashMap<String, String> params) {
        prepare(synthesizerStatus -> {

            if (synthesizerStatus != SynthesizerListener.Status.SUCCESS) {
                return;
            }

            String finalText = HtmlUtils.from(text).toString();

            for (TextFilter filter : getFilters()) {
                logger.v(TAG, "AMAZON TTS[%s] - apply filter: %s", utteranceId, filter);
                finalText = filter.apply(finalText);
            }

            final URL audioUrl = mAmazonSpeechClient.getPresignedSynthesizeSpeechUrl(
                    new SynthesizeSpeechPresignRequest().withVoiceId(mDefaultVoices.getId())
                            .withOutputFormat(OutputFormat.Mp3)
                            .withLanguageCode(mLanguageCode)
                            .withText(finalText)
            );

            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(audioUrl.toString());
                mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                    closeMediaPlayer();
                    onListenersAvailable(utteranceId, Operation.REMOVE,
                            listener -> listener.onDone(utteranceId));
                });
                mMediaPlayer.setOnErrorListener((mediaPlayer, what, extra) -> {
                    closeMediaPlayer();
                    onListenersAvailable(utteranceId, Operation.REMOVE,
                            listener -> listener.onError(utteranceId, extra));
                    return true;
                });
                mMediaPlayer.setOnPreparedListener(mediaPlayer -> {
                    onListenersAvailable(utteranceId, Operation.GET, listener -> listener.onStart(utteranceId));
                    mMediaPlayer.start();
                });
                mMediaPlayer.prepare();

            } catch (IOException ex) {
                closeMediaPlayer();
            }
        });
    }

    private void onListenersAvailable(String utteranceId,
                                      Operation operation,
                                      OnUtteranceListenerAvailable callback) {
        final SynthesizerUtteranceListener utteranceListener = operation.get(utteranceId, getListenersMap());
        if (utteranceListener != null) {
            callback.onAvailable(utteranceListener);
        }
    }

    private void closeMediaPlayer() {
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.stop();
            } catch (IllegalStateException ex) {
                // Ignore this exception...
            }
            mMediaPlayer.release();
        }
        mMediaPlayer = null;
    }

    @Override
    void playSilence(String utteranceId, long durationInMillis) {
        getSynthesizerUtteranceListener().onStart(utteranceId);
        mCondVar.block(durationInMillis);
        getSynthesizerUtteranceListener().onDone(utteranceId);
    }

    @Override
    HashMap<String, String> buildParams(String uId, String s) {
        return null;
    }

    @Override
    boolean isTtsNull() {
        return mAmazonSpeechClient == null;
    }

    @Override
    boolean isTtsSpeaking() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    @Override
    SynthesizerUtteranceListener createUtteranceListener(SynthesizerListener[] listeners) {
        return new BaseSynthesizerUtteranceListener(this) {
            @Override
            String getTtsLogLabel() {
                return LOG_LABEL;
            }
        };
    }

    @Override
    public void checkStatus(SynthesizerListener.OnStatusChecked listener) {
        prepare(synthesizerStatus -> {
            final DescribeVoicesResult voicesResult = mAmazonSpeechClient.describeVoices(new DescribeVoicesRequest()
                    .withLanguageCode(mLanguageCode));
            if (!voicesResult.getVoices().isEmpty()) {
                mDefaultVoices = voicesResult.getVoices().get(0);
                listener.execute(SynthesizerListener.Status.SUCCESS);
            } else {
                listener.execute(SynthesizerListener.Status.LANGUAGE_NOT_SUPPORTED_ERROR);
            }
        });
    }

    @Override
    public void stop() {
        super.stop();
        closeMediaPlayer();
    }

    @Override
    public void shutdown() {
        closeMediaPlayer();
        if (mAmazonSpeechClient != null) {
            mAmazonSpeechClient.shutdown();
        }
        mAmazonSpeechClient = null;
    }

    private interface OnUtteranceListenerAvailable {
        void onAvailable(SynthesizerUtteranceListener listener);
    }

    private enum Operation {
        GET {
            @Override
            SynthesizerUtteranceListener get(String utteranceId,
                                             @NonNull Map<String, SynthesizerUtteranceListener> listenerMap) {
                return listenerMap.get(utteranceId);
            }
        },
        REMOVE {
            @Override
            SynthesizerUtteranceListener get(String utteranceId,
                                             @NonNull Map<String, SynthesizerUtteranceListener> listenerMap) {
                return listenerMap.remove(utteranceId);
            }
        };

        abstract SynthesizerUtteranceListener get(String utteranceId,
                                                  @NonNull Map<String, SynthesizerUtteranceListener> listenerMap);
    }
}