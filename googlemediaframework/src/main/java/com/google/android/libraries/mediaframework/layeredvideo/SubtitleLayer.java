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

package com.google.android.libraries.mediaframework.layeredvideo;

import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.exoplayer.text.Cue;
import com.google.android.libraries.mediaframework.R;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;

import java.util.List;

/**
 * Creates a view which displays subtitles.
 */
public class SubtitleLayer implements Layer , ExoplayerWrapper.CaptionListener{

  /**
   * The text view that displays the subtitles.
   */
  private TextView subtitles;

  /**
   * The view that is created by this layer (it contains SubtitleLayer#subtitles).
   */
  private FrameLayout view;

  @Override
  public FrameLayout createView(LayerManager layerManager) {
    LayoutInflater inflater = layerManager.getActivity().getLayoutInflater();

    view = (FrameLayout) inflater.inflate(R.layout.subtitle_layer, null);
    subtitles = (TextView) view.findViewById(R.id.subtitles);
    layerManager.getExoplayerWrapper().setCaptionListener(this);

    return view;
  }

  @Override
  public void onLayerDisplayed(LayerManager layerManager) {

  }

  /**
   * When subtitles arrive, display them in the text view.
   * @param cues The subtitles that must be displayed.
   */

  @Override
  public void onCues(List<Cue> cues) {
      StringBuilder sb = new StringBuilder();
      for (Cue cue : cues){
        sb.append(cue.text);
      }
      this.subtitles.setText(sb.toString());
  }


  /**
   * Show or hide the subtitles.
   * @param visibility One of {@link android.view.View#INVISIBLE},
   *                   {@link android.view.View#VISIBLE}, {@link android.view.View#GONE}.
   */
  public void setVisibility(int visibility) {
    view.setVisibility(visibility);
  }


}
