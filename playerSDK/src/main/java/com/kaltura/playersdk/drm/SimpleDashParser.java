package com.kaltura.playersdk.drm;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer.chunk.ChunkExtractorWrapper;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.InitializationChunk;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.Representation;
import com.google.android.exoplayer.drm.DrmInitData;
import com.google.android.exoplayer.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.FileDataSource;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by noamt on 03/05/2016.
 */
class SimpleDashParser {
    private static final String TAG = "SimpleDashParser";
    DrmInitData drmInitData;
    Format format;

    public SimpleDashParser parse(String localPath) throws IOException {

        InputStream inputStream = new BufferedInputStream(new FileInputStream(localPath));

        MediaPresentationDescriptionParser mpdParser = new MediaPresentationDescriptionParser();
        MediaPresentationDescription mpd = mpdParser.parse(localPath, inputStream);


        Period period = mpd.getPeriod(0);
        List<AdaptationSet> adaptationSets = period.adaptationSets;
        AdaptationSet videoAdaptation = adaptationSets.get(period.getAdaptationSetIndex(AdaptationSet.TYPE_VIDEO));
        Representation representation = videoAdaptation.representations.get(0);

        Uri initFile = representation.getInitializationUri().getUri();


        FileDataSource initChunkSource = new FileDataSource();
        DataSpec initDataSpec = new DataSpec(initFile);
        int trigger = 2;
        format = representation.format;
        ChunkExtractorWrapper extractorWrapper = new ChunkExtractorWrapper(new FragmentedMp4Extractor());
        InitializationChunk chunk = new InitializationChunk(initChunkSource, initDataSpec, trigger, format, extractorWrapper);
        try {
            chunk.load();
        } catch (InterruptedException e) {
            Log.d(TAG, "Interrupted!", e);
        }
        if (!chunk.isLoadCanceled()) {
            drmInitData = chunk.getDrmInitData();
        }

        return this;
    }
}
