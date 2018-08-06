package com.chattylabs.sdk.android.voice;

import android.support.annotation.RawRes;

import static com.chattylabs.sdk.android.voice.ConversationalFlowComponent.SpeechRecognizer;
import static com.chattylabs.sdk.android.voice.ConversationalFlowComponent.SpeechSynthesizer;

public class ComponentConfig {
    static final String SYNTHESIZER_SERVICE_ANDROID = "AndroidSpeechSynthesizer";
    static final String RECOGNIZER_SERVICE_ANDROID = "AndroidSpeechRecognizer";
    static final String SYNTHESIZER_SERVICE_GOOGLE = "GoogleSpeechSynthesizer";
    static final String RECOGNIZER_SERVICE_GOOGLE = "GoogleSpeechRecognizer";

    private LazyProvider<Boolean> bluetoothScoRequired;
    private LazyProvider<Boolean> audioExclusiveRequiredForSynthesizer;
    private LazyProvider<Boolean> audioExclusiveRequiredForRecognizer;
    private LazyProvider<Class<? extends SpeechRecognizer>> recognizerServiceType;
    private LazyProvider<Class<? extends SpeechSynthesizer>> synthesizerServiceType;
    private RawResourceLazyProvider googleCredentialsResourceFile;

    private ComponentConfig(Builder builder) {
        bluetoothScoRequired = builder.bluetoothScoRequired;
        audioExclusiveRequiredForSynthesizer = builder.audioExclusiveRequiredForSynthesizer;
        audioExclusiveRequiredForRecognizer = builder.audioExclusiveRequiredForRecognizer;
        recognizerServiceType = builder.recognizerServiceType;
        synthesizerServiceType = builder.synthesizerServiceType;
        googleCredentialsResourceFile = builder.googleCredentialsResourceFile;
    }

    public boolean isBluetoothScoRequired() {
        return bluetoothScoRequired.get();
    }

    public boolean isAudioExclusiveRequiredForSynthesizer() {
        return audioExclusiveRequiredForSynthesizer.get();
    }

    public boolean isAudioExclusiveRequiredForRecognizer() {
        return audioExclusiveRequiredForRecognizer.get();
    }

    public Class<? extends SpeechRecognizer> getRecognizerServiceType() {
        return recognizerServiceType.get();
    }

    public Class<? extends SpeechSynthesizer> getSynthesizerServiceType() {
        return synthesizerServiceType.get();
    }

    @RawRes
    public int getGoogleCredentialsResourceFile() {
        return googleCredentialsResourceFile.get();
    }

    public static final class Builder {
        private LazyProvider<Boolean> bluetoothScoRequired;
        private LazyProvider<Boolean> audioExclusiveRequiredForSynthesizer;
        private LazyProvider<Boolean> audioExclusiveRequiredForRecognizer;
        private LazyProvider<Class<? extends SpeechRecognizer>> recognizerServiceType;
        private LazyProvider<Class<? extends SpeechSynthesizer>> synthesizerServiceType;
        private RawResourceLazyProvider googleCredentialsResourceFile;

        public Builder() {
        }

        public Builder(ComponentConfig copy) {
            bluetoothScoRequired = copy.bluetoothScoRequired;
            audioExclusiveRequiredForSynthesizer = copy.audioExclusiveRequiredForSynthesizer;
            audioExclusiveRequiredForRecognizer = copy.audioExclusiveRequiredForRecognizer;
            recognizerServiceType = copy.recognizerServiceType;
            synthesizerServiceType = copy.synthesizerServiceType;
            googleCredentialsResourceFile = copy.googleCredentialsResourceFile;
        }

        public Builder setBluetoothScoRequired(LazyProvider<Boolean> lazyProvider) {
            this.bluetoothScoRequired = lazyProvider;
            return this;
        }

        public Builder setAudioExclusiveRequiredForSynthesizer(LazyProvider<Boolean> lazyProvider) {
            this.audioExclusiveRequiredForSynthesizer = lazyProvider;
            return this;
        }

        public Builder setAudioExclusiveRequiredForRecognizer(LazyProvider<Boolean> lazyProvider) {
            this.audioExclusiveRequiredForRecognizer = lazyProvider;
            return this;
        }

        public Builder setRecognizerServiceType(
                LazyProvider<Class<? extends SpeechRecognizer>> lazyRecognizerServiceType) {
            this.recognizerServiceType = lazyRecognizerServiceType;
            return this;
        }

        public Builder setSynthesizerServiceType(
                LazyProvider<Class<? extends SpeechSynthesizer>> lazyRecognizerServiceType) {
            this.synthesizerServiceType = lazyRecognizerServiceType;
            return this;
        }

        public Builder setGoogleCredentialsResourceFile(RawResourceLazyProvider lazyProvider) {
            this.googleCredentialsResourceFile = lazyProvider;
            return this;
        }

        public ComponentConfig build() {
            return new ComponentConfig(this);
        }
    }

    public interface Update {
        ComponentConfig run(ComponentConfig.Builder builder);
    }

    public interface LazyProvider<T> {
        T get();
    }

    public interface RawResourceLazyProvider {
        @RawRes int get();
    }
}
