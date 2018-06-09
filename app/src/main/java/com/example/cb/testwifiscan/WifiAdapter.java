package com.example.cb.testwifiscan;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cb on 2018/6/7.
 */

public class WifiAdapter extends RecyclerView.Adapter {


    List<ScanResult> list = new ArrayList<>();
    Context context;
    ItemClick itemClick;

    public void setItemClick(ItemClick itemClick) {
        this.itemClick = itemClick;
    }

    public void setList(List<ScanResult> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public WifiAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wifi, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        WifiViewHolder viewHolder = (WifiViewHolder) holder;
        ScanResult result = list.get(position);

        boolean hasPassWord = false;
        WifiConnectManager.WifiCipherType cipherType = WifiConnectManager.getCipherType(result);
        if (cipherType == WifiConnectManager.WifiCipherType.WIFICIPHER_WEP ||
                cipherType == WifiConnectManager.WifiCipherType.WIFICIPHER_WPA) {
            hasPassWord = true;
        }
        String extra = hasPassWord ? "  有密码" : "";
        viewHolder.tvName.setText(result.SSID + extra);
        viewHolder.tvLevel.setText(String.valueOf(Math.abs(result.level)));

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClick != null) {
                    itemClick.onClick(list.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class WifiViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvLevel;

        public WifiViewHolder(final View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLevel = itemView.findViewById(R.id.tv_level);
        }
    }

    public interface ItemClick{
        void onClick(ScanResult result);
    }

}
