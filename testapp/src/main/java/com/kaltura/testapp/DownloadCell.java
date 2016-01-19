package com.kaltura.testapp;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;

/**
 * Created by nissimpardo on 03/01/16.
 */
public class DownloadCell extends RelativeLayout {
    HashMap<String, Object> mParams;

    public DownloadCell(Context context) {
        super(context);
    }

    public DownloadCell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DownloadCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public TextView getTextView() {
        return (TextView)findViewById(R.id.textView2);
    }

    public ProgressBar getProgressBar() {
        return (ProgressBar)findViewById(R.id.progressBar);
    }

    public void updateProgress(float progress) {
        getProgressBar().setProgress((int)(progress * 100));
    }

    public void setCellState(boolean isInputState) {
        if (isInputState) {
            getProgressBar().setProgress(100);
            getTextView().setVisibility(VISIBLE);
            getProgressBar().setVisibility(INVISIBLE);
        } else {
            getProgressBar().setProgress(0);
            getTextView().setVisibility(INVISIBLE);
            getProgressBar().setVisibility(VISIBLE);
        }
    }


}
