package com.kaltura.testapp;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;

/**
 * TODO: document your custom view class.
 */
public class InputCellView extends RelativeLayout implements TextWatcher {
    HashMap<String, Object> mParams;
    InputCellListener mListener;

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mListener != null && s != null && s.length() > 0) {
            mListener.textDidChanged(this, s.toString());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public interface InputCellListener {
        void textDidChanged(View v, String text);
    }
    public InputCellView(Context context) {
        super(context);
    }

    public InputCellView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        EditText editText = (EditText)findViewById(R.id.editText);
        editText.addTextChangedListener(this);
    }

    public InputCellView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setParams(HashMap<String, Object> params) {
        mParams = params;
        ((TextView)findViewById(R.id.textView)).setText((String) params.get("title"));

    }

    public void setInputCellListener(InputCellListener listener) {
        if (mListener == null) {
            mListener = listener;
        }
    }

    public String getTitle() {
        return (String)mParams.get("title");
    }
}
