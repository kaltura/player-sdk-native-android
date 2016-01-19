package com.kaltura.testapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nissimpardo on 03/01/16.
 */
public class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.ItemViewHolder> {
    private ArrayList<HashMap<String, Object>> mDataset;
    private static MyClickListener myClickListener;

    public static class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, InputCellView.InputCellListener {
        private View mView;
        private HashMap<String, Object> mParams;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setParams(HashMap<String, Object> params) {
            mParams = params;
            int viewType = ((Integer)params.get("cellType")).intValue();
            if (viewType == 1) {
                TextView textView = (TextView) mView.findViewById(R.id.textView2);
                textView.setText((String) params.get("title"));
                mView.setOnClickListener(this);
            } else {
                ((InputCellView) mView).setInputCellListener(this);
                EditText editText = (EditText)mView.findViewById(R.id.editText);
                editText.setHint((String) params.get("title"));
                if (params.get("value") != null) {
                    editText.setText((String)params.get("value"));
                }
            }
        }


        @Override
        public void onClick(View v) {
            if (myClickListener != null) {
                myClickListener.onRowClick((DownloadCell)v, ((Integer)mParams.get("position")).intValue());
            }
        }

        @Override
        public void textDidChanged(View v, String text) {
            if (myClickListener != null) {
                myClickListener.onTextChanged((String)mParams.get("title"), text);
            }
        }
    }
    private Context mContext;
    private HashMap<String, String> mConfig;

    public DemoAdapter(Context context, ArrayList<HashMap<String, Object>> items) {
        super();
        mContext = context;
        mDataset = items;
    }

    public  void setOnItemClickListener(MyClickListener clickListener) {
        myClickListener = clickListener;
    }

    public void loadRemoteConfig(HashMap<String, String> config) {
        mConfig = config;
        notifyDataSetChanged();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int id = viewType == 0 ? R.layout.input_cell : R.layout.action_cell;
        View view = LayoutInflater.from(parent.getContext()).inflate(id, parent, false);
        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        HashMap<String, Object> current = mDataset.get(position);
        current.put("position", new Integer(position));
        if (mConfig != null) {
            current.put("value", mConfig.get(current.get("title")));
        }
        holder.setParams(current);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = ((Integer)mDataset.get(position).get("cellType")).intValue();
        return viewType;
    }


    public interface MyClickListener {
        void onRowClick(DownloadCell view, int position);
        void onTextChanged(String key, String value);
    }


}
