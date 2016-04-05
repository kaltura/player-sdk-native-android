/**
 Copyright 2014 Google Inc. All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.google.android.libraries.mediaframework.exoplayerextensions;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer.drm.MediaDrmCallback;

/**
 * Generate a renderer builder appropriate for rendering a video.
 */
public class RendererBuilderFactory {

  /**
   * Create a renderer builder which can build the given video.
   * @param ctx The context (ex {@link android.app.Activity} in whicb the video has been created.
   * @param video The video which will be played.
   * @param mediaDrmCallback DRM Callback.
   * @param preferSoftwareDecoder true if softwareDecoder is requred.
   */
  public static ExoplayerWrapper.RendererBuilder createRendererBuilder(Context ctx,
                                                                       Video video,
                                                                       MediaDrmCallback mediaDrmCallback,
                                                                       boolean preferSoftwareDecoder) {
    switch (video.getVideoType()) {
      case HLS:
        return new HlsRendererBuilder(ctx, ExoplayerUtil.getUserAgent(ctx),
                                      video.getUrl());
      case DASH:
        return new DashRendererBuilder(ctx, ExoplayerUtil.getUserAgent(ctx),
                                       video.getUrl(),
                                       mediaDrmCallback);
      case MP4:
        return new ExtractorRendererBuilder(ctx, ExoplayerUtil.getUserAgent(ctx), Uri.parse(video.getUrl()), preferSoftwareDecoder);
      case OTHER:
        return new ExtractorRendererBuilder(ctx, ExoplayerUtil.getUserAgent(ctx), Uri.parse(video.getUrl()), preferSoftwareDecoder);
      default:
        return null;
    }
  }


    /**
     * Create a renderer builder which can build the given video.
     * @param ctx The context (ex {@link android.app.Activity} in whicb the video has been created.
     * @param video The video which will be played.
     */
    public static ExoplayerWrapper.RendererBuilder createRendererBuilder(Context ctx,
                                                                         Video video,
                                                                         MediaDrmCallback mediaDrmCallback) {
        return createRendererBuilder(ctx, video, mediaDrmCallback, false);
    }

  public static ExoplayerWrapper.RendererBuilder createRendererBuilder(Context ctx,
                                                                       Video video,
                                                                       boolean preferSoftwareDecoder) {
    
    return createRendererBuilder(ctx, video, null, preferSoftwareDecoder);
  }

    public static ExoplayerWrapper.RendererBuilder createRendererBuilder(Context ctx,
                                                                         Video video) {

        return createRendererBuilder(ctx, video, null, false);
    }
}
