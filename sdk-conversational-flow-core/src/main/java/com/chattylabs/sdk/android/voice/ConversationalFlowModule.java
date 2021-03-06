package com.chattylabs.sdk.android.voice;


import com.chattylabs.android.commons.internal.ILogger;

public abstract class ConversationalFlowModule {

    public static ConversationalFlowComponent provideComponent(ILogger logger) {
        logger.setBuildDebug(BuildConfig.DEBUG);
        ConversationalFlowComponent component = ConversationalFlowComponentImpl.Instance.get();
        ((ConversationalFlowComponentImpl)component).setLogger(logger);
        return component;
    }
}
