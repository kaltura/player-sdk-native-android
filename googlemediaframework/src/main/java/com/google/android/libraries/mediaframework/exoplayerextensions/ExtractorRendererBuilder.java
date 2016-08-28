/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.google.android.libraries.mediaframework.exoplayerextensions;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer.DecoderInfo;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper.RendererBuilder;
import com.google.android.libraries.mediaframework.layeredvideo.Util;

/**
 * A {@link RendererBuilder} for streams that can be read using an {@link Extractor}.
 */
public class ExtractorRendererBuilder implements RendererBuilder {
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final String userAgent;
    private final Uri uri;
    private final boolean preferSoftwareDecoder;

    public ExtractorRendererBuilder(Context context, String userAgent, Uri uri, boolean preferSoftwareDecoder) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
        this.preferSoftwareDecoder = preferSoftwareDecoder;
    }

    private MediaCodecSelector preferSoftwareMediaCodecSelector = new MediaCodecSelector() {
        @Override
        public DecoderInfo getDecoderInfo(String mimeType, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
            Log.d("Kaltura", "DeviceInfo: " + Util.getDeviceInfo() + ", mimeType:" + mimeType + ", requiresSecureDecoder" + requiresSecureDecoder);

            if (!requiresSecureDecoder && !isVendorSupportDefaultDecoder()) {
                DecoderInfo decoderInfo = MediaCodecUtil.getDecoderInfo(mimeType, requiresSecureDecoder);
                Log.d("Kaltura", "Using Decoder = " + decoderInfo.name);
                return  decoderInfo;
            }
            Log.d("Kaltura", "Using Default Decoder");
            return MediaCodecSelector.DEFAULT.getDecoderInfo(mimeType,requiresSecureDecoder);
        }

        @Override
        public DecoderInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
            return MediaCodecSelector.DEFAULT.getPassthroughDecoderInfo();
        }
    };

    private boolean isVendorSupportDefaultDecoder(){
        if (android.os.Build.MANUFACTURER.equals("LGE")){
            return true;
        }else {
            return false;
        }
    }
    
    @Override
    public void buildRenderers(ExoplayerWrapper player) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        MediaCodecSelector mediaCodecSelector = preferSoftwareDecoder ? preferSoftwareMediaCodecSelector : MediaCodecSelector.DEFAULT;

        // Build the video and audio renderers.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);
        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                sampleSource, mediaCodecSelector, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, player.getMainHandler(),
                player, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                mediaCodecSelector, null, true, player.getMainHandler(), player,
                AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);

        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper());

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[ExoplayerWrapper.RENDERER_COUNT];
        renderers[ExoplayerWrapper.TYPE_VIDEO] = videoRenderer;
        renderers[ExoplayerWrapper.TYPE_AUDIO] = audioRenderer;
        renderers[ExoplayerWrapper.TYPE_TEXT] = textRenderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

    @Override
    public void cancel() {
        // Do nothing.
    }
}
