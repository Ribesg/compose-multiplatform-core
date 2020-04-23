/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.view;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.view.preview.transform.PreviewTransform;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class TextureViewImplementationTest {

    private static final int ANY_WIDTH = 1600;
    private static final int ANY_HEIGHT = 1200;
    private static final Size ANY_SIZE = new Size(ANY_WIDTH, ANY_HEIGHT);

    private FrameLayout mParent;
    private TextureViewImplementation mImplementation;
    private SurfaceTexture mSurfaceTexture;
    private SurfaceRequest mSurfaceRequest;

    @Before
    public void setUp() {
        final Context mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mSurfaceTexture = new SurfaceTexture(0);
        mImplementation = new TextureViewImplementation();
        mParent = new FrameLayout(mContext);
        mImplementation.init(mParent, new PreviewTransform());
    }

    @After
    public void tearDown() {
        if (mSurfaceRequest != null) {
            mSurfaceRequest.willNotProvideSurface();
            // Ensure all successful requests have their returned future finish.
            mSurfaceRequest.getDeferrableSurface().close();
            mSurfaceRequest = null;
        }
    }

    @LargeTest
    @Test(expected = TimeoutException.class)
    public void doNotProvideSurface_ifSurfaceTextureNotAvailableYet() throws Exception {
        SurfaceRequest surfaceRequest = getSurfaceRequest();
        mImplementation.getSurfaceProvider().onSurfaceRequested(surfaceRequest);
        surfaceRequest.getDeferrableSurface().getSurface().get(2, TimeUnit.SECONDS);
    }

    @Test
    public void provideSurface_ifSurfaceTextureAvailable() throws Exception {
        SurfaceRequest surfaceRequest = getSurfaceRequest();
        mImplementation.getSurfaceProvider().onSurfaceRequested(surfaceRequest);
        final ListenableFuture<Surface> surfaceListenableFuture =
                surfaceRequest.getDeferrableSurface().getSurface();

        mImplementation.mTextureView
                .getSurfaceTextureListener()
                .onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        final Surface surface = surfaceListenableFuture.get();
        assertThat(surface).isNotNull();
        assertThat(mImplementation.mSurfaceRequest).isNull();
    }

    @Test
    public void doNotDestroySurface_whenSurfaceTextureBeingDestroyed_andCameraUsingSurface()
            throws Exception {
        SurfaceRequest surfaceRequest = getSurfaceRequest();
        mImplementation.getSurfaceProvider().onSurfaceRequested(surfaceRequest);
        final ListenableFuture<Surface> surfaceListenableFuture =
                surfaceRequest.getDeferrableSurface().getSurface();

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        surfaceListenableFuture.get();

        assertThat(mImplementation.mSurfaceReleaseFuture).isNotNull();
        assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture)).isFalse();
    }

    @Test
    @LargeTest
    public void destroySurface_whenSurfaceTextureBeingDestroyed_andCameraNotUsingSurface()
            throws Exception {
        SurfaceRequest surfaceRequest = getSurfaceRequest();
        mImplementation.getSurfaceProvider().onSurfaceRequested(surfaceRequest);
        DeferrableSurface deferrableSurface = surfaceRequest.getDeferrableSurface();
        final ListenableFuture<Surface> surfaceListenableFuture = deferrableSurface.getSurface();

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        surfaceListenableFuture.get();
        deferrableSurface.close();

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertThat(mImplementation.mSurfaceReleaseFuture).isNull();
        assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture)).isTrue();
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    @LargeTest
    public void releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface_1()
            throws Exception {
        SurfaceRequest surfaceRequest = getSurfaceRequest();
        mImplementation.getSurfaceProvider().onSurfaceRequested(surfaceRequest);
        DeferrableSurface deferrableSurface = surfaceRequest.getDeferrableSurface();
        final ListenableFuture<Surface> surfaceListenableFuture = deferrableSurface.getSurface();

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        surfaceListenableFuture.get();

        surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture);
        deferrableSurface.close();

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertThat(mImplementation.mSurfaceReleaseFuture).isNull();
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @LargeTest
    public void releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface_2()
            throws Exception {
        SurfaceRequest surfaceRequest = getSurfaceRequest();
        mImplementation.getSurfaceProvider().onSurfaceRequested(surfaceRequest);
        DeferrableSurface deferrableSurface = surfaceRequest.getDeferrableSurface();
        final ListenableFuture<Surface> surfaceListenableFuture = deferrableSurface.getSurface();

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        surfaceListenableFuture.get();

        surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture);
        deferrableSurface.close();

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertThat(mImplementation.mSurfaceReleaseFuture).isNull();
        assertThat(mSurfaceTexture.isReleased()).isTrue();
    }

    @Test
    @LargeTest
    public void nullSurfaceCompleterAndSurfaceReleaseFuture_whenSurfaceProviderCancelled()
            throws Exception {
        SurfaceRequest surfaceRequest = getSurfaceRequest();
        mImplementation.getSurfaceProvider().onSurfaceRequested(surfaceRequest);
        // Cancel the request from the camera side
        surfaceRequest.getDeferrableSurface().getSurface().cancel(true);

        // Wait enough time for mCompleter's cancellation listener to be called
        Thread.sleep(1_000);

        assertThat(mImplementation.mSurfaceRequest).isNull();
        assertThat(mImplementation.mSurfaceReleaseFuture).isNull();
    }

    @FlakyTest
    @Test
    public void releaseSurface_whenSurfaceTextureDestroyed_andCameraSurfaceRequestIsCancelled() {
        mImplementation.getSurfaceProvider().onSurfaceRequested(getSurfaceRequest());
        // Cancel the request from the client side
        mSurfaceRequest.willNotProvideSurface();

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture)).isTrue();
        assertThat(mImplementation.mSurfaceTexture).isNull();
    }

    @Test
    public void doNotCreateTextureView_beforeSensorOutputSizeKnown() {
        assertThat(mParent.getChildCount()).isEqualTo(0);
    }

    @Test
    public void keepOnlyLatestTextureView_whenGetSurfaceProviderCalledMultipleTimes() {
        mImplementation.getSurfaceProvider().onSurfaceRequested(getSurfaceRequest());

        assertThat(mParent.getChildAt(0)).isInstanceOf(TextureView.class);
        final TextureView textureView1 = (TextureView) mParent.getChildAt(0);

        mImplementation.getSurfaceProvider().onSurfaceRequested(getSurfaceRequest());

        assertThat(mParent.getChildAt(0)).isInstanceOf(TextureView.class);
        final TextureView textureView2 = (TextureView) mParent.getChildAt(0);

        assertThat(textureView1).isNotSameInstanceAs(textureView2);
        assertThat(mParent.getChildCount()).isEqualTo(1);
    }

    @NonNull
    private SurfaceRequest getSurfaceRequest() {
        if (mSurfaceRequest == null) {
            mSurfaceRequest = new SurfaceRequest(ANY_SIZE);
        }

        return mSurfaceRequest;
    }
}
