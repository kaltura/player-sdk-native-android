package com.kaltura.multipledtgplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
    private DownloadState mDownloadState;
    private Button mDownloadBtn;
    private OnItemListener mOnItemListener;

    public interface OnItemListener {
        void onCheck(int itemId, boolean isChecked);
        void onDownloadClick(int itemId, DownloadState downloadState);
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
        mDownloadBtn = (Button)findViewById(R.id.download_btn);
        mDownloadBtn.setOnClickListener(this);
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
                    mDownloadState = downloadItem.getState();
                    updateStatesButton();
                    mStatus.setText(mDownloadState == null ? "" : mDownloadState.name());
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
                    mOnItemListener.onDownloadClick(mItemId, mDownloadState);
                }
                break;
            case R.id.checkBox:
                if (mOnItemListener != null) {
                    mOnItemListener.onCheck(mItemId, mCheckBox.isChecked());
                }
                break;
        }
    }

    private void updateStatesButton() {
        mDownloadBtn.setVisibility(VISIBLE);
        switch (mDownloadState) {
            case NEW:
            case INFO_LOADED:
                mDownloadBtn.setText("Start download");
                break;
            case PAUSED:
                mDownloadBtn.setText("Resume download");
                break;
            case IN_PROGRESS:
                mDownloadBtn.setText("Pause download");
                break;
            case COMPLETED:
                mDownloadBtn.setVisibility(GONE);
                break;
        }
    }

    public void updateProgress(long current, long expected) {
        int progress = 0;
        if (expected != 0) {
            progress = (int)((float)current/(float)expected * 100f);
        }
       updateProgress(progress);
    }

    public void updateProgress(int progress) {
        if (progress < 0) {
            progress = 0;
        }
        else if (progress > 100) {
            progress = 100;
        }
        LOGD("update progress", progress + "%");
        mProgressBar.setProgress(progress);
    }
}
