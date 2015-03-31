package com.kaltura.hlsplayersdk;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * The actual surface. Some devices require very specific initialization
 * behavior, so this class does as little as possible - it exists just to route
 * the Surface it manages to the controller and handle layout.
 */
public class PlayerView extends SurfaceView implements SurfaceHolder.Callback {
	public HLSPlayerViewController mOwner;
	public int mVideoWidth = 640, mVideoHeight = 480;

	// Class Methods
	public PlayerView(Context context, HLSPlayerViewController owner,
			boolean wantPushBuffers) {
		super(context);

		// Set some properties on the SurfaceHolder.
		getHolder().addCallback(this);
		getHolder().setKeepScreenOn(true);

		// Pre 3.0 we must set this explicitly.
		if ((android.os.Build.VERSION.SDK_INT < 11) && wantPushBuffers) {
			Log.i("PlayerView",
					"Explcitly setting surface type to push on pre-api 11 device.");
			getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		mOwner = owner;
	}

	// Layout logic.
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) +
		// ", "
		// + MeasureSpec.toString(heightMeasureSpec) + ")");

		int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
		if ((mVideoWidth > 0) && (mVideoHeight > 0)) {

			int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
			int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
			int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
			int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

			if ((widthSpecMode == MeasureSpec.EXACTLY)
					&& (heightSpecMode == MeasureSpec.EXACTLY)) {
				// the size is fixed
				width = widthSpecSize;
				height = heightSpecSize;

				// for compatibility, we adjust size based on aspect ratio
				if ((mVideoWidth * height) < (width * mVideoHeight)) {
					// Log.i("@@@", "image too wide, correcting");
					width = (height * mVideoWidth) / mVideoHeight;
				} else if ((mVideoWidth * height) > (width * mVideoHeight)) {
					// Log.i("@@@", "image too tall, correcting");
					height = (width * mVideoHeight) / mVideoWidth;
				}
			} else if (widthSpecMode == MeasureSpec.EXACTLY) {
				// only the width is fixed, adjust the height to match aspect
				// ratio if possible
				width = widthSpecSize;
				height = (width * mVideoHeight) / mVideoWidth;
				if ((heightSpecMode == MeasureSpec.AT_MOST)
						&& (height > heightSpecSize)) {
					// couldn't match aspect ratio within the constraints
					height = heightSpecSize;
				}
			} else if (heightSpecMode == MeasureSpec.EXACTLY) {
				// only the height is fixed, adjust the width to match aspect
				// ratio if possible
				height = heightSpecSize;
				width = (height * mVideoWidth) / mVideoHeight;
				if ((widthSpecMode == MeasureSpec.AT_MOST)
						&& (width > widthSpecSize)) {
					// couldn't match aspect ratio within the constraints
					width = widthSpecSize;
				}
			} else {
				// neither the width nor the height are fixed, try to use actual
				// video size
				width = mVideoWidth;
				height = mVideoHeight;
				if ((heightSpecMode == MeasureSpec.AT_MOST)
						&& (height > heightSpecSize)) {
					// too tall, decrease both width and height
					height = heightSpecSize;
					width = (height * mVideoWidth) / mVideoHeight;
				}
				if ((widthSpecMode == MeasureSpec.AT_MOST)
						&& (width > widthSpecSize)) {
					// too wide, decrease both width and height
					width = widthSpecSize;
					height = (width * mVideoHeight) / mVideoWidth;
				}
			}
		} else {
			// no size yet, just adopt the given spec sizes
		}
		setMeasuredDimension(width, height);
	}

	// SurfaceHolder.Callback implementation
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i("PlayerView", "Saw new format " + format + " " + width + "x"
				+ height + " on surface " + holder.getSurface());
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i("PlayerView.surfaceCreated", "**** Surface created.");
		mOwner.SetSurface(this.getHolder().getSurface());
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i("PlayerView.surfaceDestroyed", "**** Surface destroyed.");
		mOwner.SetSurface(null);
	}
}
