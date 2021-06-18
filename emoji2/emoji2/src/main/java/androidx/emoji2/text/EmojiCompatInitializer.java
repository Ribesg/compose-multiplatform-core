/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.emoji2.text;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.view.Choreographer;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.core.os.TraceCompat;
import androidx.startup.Initializer;

import java.util.Collections;
import java.util.List;

/**
 * Initializer for configuring EmojiCompat with the system installed downloadable font provider.
 *
 * <p>This initializer will initialize EmojiCompat immediately then defer loading the font for a
 * short delay to avoid delaying application startup. Typically, the font will be loaded shortly
 * after the first screen of your application loads, which means users may see system emoji
 * briefly prior to the compat font loading.</p>
 *
 * <p>This is the recommended configuration for all apps that don't need specialized configuration,
 * and don't need to control the background thread that initialization runs on. For more information
 * see {@link androidx.emoji2.text.DefaultEmojiCompatConfig}.</p>
 *
 * <p>In addition to the reasons listed in {@code DefaultEmojiCompatConfig} you may wish to disable
 * this automatic configuration if you intend to call initialization from an existing background
 * thread pool in your application.</p>
 *
 * <p>This is enabled by default by including the {@code :emoji2:emoji2} gradle artifact. To
 * disable the default configuration (and allow manual configuration) add this to your manifest:</p>
 *
 * <pre>
 *     <provider
 *         android:name="androidx.startup.InitializationProvider"
 *         android:authorities="${applicationId}.androidx-startup"
 *         android:exported="false"
 *         tools:node="merge">
 *         <meta-data android:name="androidx.emoji2.text.EmojiCompatInitializer"
 *                   tools:node="remove" />
 *     </provider>
 * </pre>
 *
 * @see androidx.emoji2.text.DefaultEmojiCompatConfig
 */
public class EmojiCompatInitializer implements Initializer<Boolean> {
    private static final long STARTUP_THREAD_CREATION_DELAY_MS = 500L;
    private static final String S_INITIALIZER_THREAD_NAME = "EmojiCompatInitializer";

    /**
     * Initialize EmojiCompat with the app's context.
     *
     * @param context application context
     * @return result of default init
     */
    @NonNull
    @Override
    public Boolean create(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 19) {
            EmojiCompat.init(new BackgroundDefaultConfig(context));
            delayAfterFirstFrame();
            return true;
        }
        return false;
    }

    /**
     * Wait until the first frame of the application to do anything.
     *
     * This allows startup code to run before the delay is scheduled.
     */
    @RequiresApi(19)
    void delayAfterFirstFrame() {
        // schedule delay after first frame callback
        Choreographer16Impl.postFrameCallback(this::loadEmojiCompatAfterDelay);
    }

    @RequiresApi(19)
    private void loadEmojiCompatAfterDelay() {
        final Handler mainHandler;
        if (Build.VERSION.SDK_INT >= 28) {
            mainHandler = Handler28Impl.createAsync(Looper.getMainLooper());
        } else {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.postDelayed(new LoadEmojiCompatRunnable(),
                STARTUP_THREAD_CREATION_DELAY_MS);
    }

    /**
     * No dependencies
     */
    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }

    static class LoadEmojiCompatRunnable implements Runnable {
        @Override
        public void run() {
            try {
                // this is main thread, so mark what we're doing (this trace includes thread
                // start time in BackgroundLoadingLoader.load
                TraceCompat.beginSection("EmojiCompat.EmojiCompatInitializer.run");
                if (EmojiCompat.isConfigured()) {
                    EmojiCompat.get().load();
                }
            } finally {
                TraceCompat.endSection();
            }
        }
    }

    @RequiresApi(19)
    static class BackgroundDefaultConfig extends EmojiCompat.Config {
        protected BackgroundDefaultConfig(Context context) {
            super(new BackgroundDefaultLoader(context));
            setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL);
        }
    }

    @RequiresApi(19)
    static class BackgroundDefaultLoader implements EmojiCompat.MetadataRepoLoader {
        private final Context mContext;

        BackgroundDefaultLoader(Context context) {
            mContext = context.getApplicationContext();
        }

        @Nullable
        private HandlerThread mThread;

        @Override
        public void load(@NonNull EmojiCompat.MetadataRepoLoaderCallback loaderCallback) {
            Handler handler = getThreadHandler();
            handler.post(() -> doLoad(loaderCallback, handler));
        }

        @WorkerThread
        void doLoad(@NonNull EmojiCompat.MetadataRepoLoaderCallback loaderCallback,
                @NonNull Handler handler) {
            try {
                FontRequestEmojiCompatConfig config = DefaultEmojiCompatConfig.create(mContext);
                if (config == null) {
                    throw new RuntimeException("EmojiCompat font provider not available on this "
                            + "device.");
                }
                config.setHandler(handler);
                config.getMetadataRepoLoader().load(new EmojiCompat.MetadataRepoLoaderCallback() {
                    @Override
                    public void onLoaded(@NonNull MetadataRepo metadataRepo) {
                        try {
                            // main thread is notified before returning, so we can quit now
                            loaderCallback.onLoaded(metadataRepo);
                        } finally {
                            quitHandlerThread();
                        }
                    }

                    @Override
                    public void onFailed(@Nullable Throwable throwable) {
                        try {
                            // main thread is notified before returning, so we can quit now
                            loaderCallback.onFailed(throwable);
                        } finally {
                            quitHandlerThread();
                        }
                    }
                });
            } catch (Throwable t) {
                loaderCallback.onFailed(t);
                quitHandlerThread();
            }
        }

        void quitHandlerThread() {
            if (mThread != null) {
                mThread.quitSafely();
            }
        }

        @NonNull
        private Handler getThreadHandler() {
            mThread = new HandlerThread(S_INITIALIZER_THREAD_NAME,
                    Process.THREAD_PRIORITY_BACKGROUND);
            mThread.start();
            return new Handler(mThread.getLooper());
        }
    }

    @RequiresApi(16)
    private static class Choreographer16Impl {
        private Choreographer16Impl() {
            // Non-instantiable.
        }

        @DoNotInline
        public static void postFrameCallback(Runnable r) {
            Choreographer.getInstance().postFrameCallback(frameTimeNanos -> r.run());
        }
    }

    @RequiresApi(28)
    private static class Handler28Impl {
        private Handler28Impl() {
            // Non-instantiable.
        }

        @DoNotInline
        public static Handler createAsync(Looper looper) {
            return Handler.createAsync(looper);
        }
    }
}
