package com.kaltura.multipledtgplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kaltura.dtg.DownloadItem;
import com.kaltura.dtg.DownloadState;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;

/**
 * Created by Gleb on 9/29/16.
 */

public class DownloadItemView extends RelativeLayout implements View.OnClickListener {

    private CheckBox mCheckBox;
    private ProgressBar mProgressBar;
    private TextView mStatus;
    private int mItemId;
    private OnItemListener mOnItemListener;

    public interface OnItemListener {
        void onCheck(int itemId, boolean isChecked);
        void onDownloadClick(int itemId);
    }

    public DownloadItemView(Context context) {
        super(context);
        init();
    }

    public DownloadItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DownloadItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DownloadItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.download_item_layout, this);
        mCheckBox = (CheckBox) findViewById(R.id.checkBox);
        mProgressBar = (ProgressBar) findViewById(R.id.download_progress);
        mStatus = (TextView) findViewById(R.id.status);
        findViewById(R.id.download_btn).setOnClickListener(this);
        mCheckBox.setOnClickListener(this);
    }

    public void bind(VideoItem videoItem) {
        mCheckBox.setText(videoItem.name);
        mCheckBox.setChecked(videoItem.isSelected);
        bind(videoItem.findDownloadItem());
    }

    public void bind(final DownloadItem downloadItem) {
        post(new Runnable() {
            @Override
            public void run() {
                if (downloadItem == null) {
                    mStatus.setText("");
                }
                else {
                    DownloadState state = downloadItem.getState();
                    mStatus.setText(state == null ? "" : state.name());
                    updateProgress(downloadItem.getDownloadedSizeBytes(), downloadItem.getEstimatedSizeBytes());
                }
            }
        });
    }

    public void setItemId(int itemId) {
        mItemId = itemId;
    }

    public void setOnItemListener(OnItemListener onItemListener) {
        mOnItemListener = onItemListener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.download_btn:
                if (mOnItemListener != null) {
                    mOnItemListener.onDownloadClick(mItemId);
                }
                break;
            case R.id.checkBox:
                if (mOnItemListener != null) {
                    mOnItemListener.onCheck(mItemId, mCheckBox.isChecked());
                }
                break;
        }
    }

    public void updateProgress(long current, long expected) {
        int progress = 0;
        if (expected != 0) {
            progress = (int)((float)current/(float)expected * 100f);
        }
        LOGD("update progress", progress + "%");
        mProgressBar.setProgress(progress);
    }

    public void updateProgress(int progress) {
        mProgressBar.setProgress(progress);
    }
}
