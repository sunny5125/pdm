package com.disarm.sanna.pdm.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.disarm.sanna.pdm.R;
import com.disarm.sanna.pdm.Senders;

import java.util.ArrayList;

/**
 * Created by arka on 19/9/16.
 * Adapter for Recycle View in SocialShareActivity
 */
public class SocialShareChatlistAdapter extends
        RecyclerView.Adapter<SocialShareChatlistAdapter.ViewHolder>{
    private ArrayList<String> numbers;
    private ArrayList<String> names;
    private ArrayList<String> time;

    public SocialShareChatlistAdapter(ArrayList<String> numbers,
                                      ArrayList<String> names) {
        this.numbers = numbers;
        this.names = names;
    }

    @Override
    public SocialShareChatlistAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                    int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.social_share_chats_row, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.number.setText(numbers.get(position));
        holder.name.setText(names.get(position));
    }

    @Override
    public int getItemCount() {
        return numbers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView number;
        public TextView name;
        public TextView time;

        public ViewHolder(View itemView) {
            super(itemView);
            number = (TextView)itemView.findViewById(R.id.social_share_chats_row_number);
            name = (TextView)itemView.findViewById(R.id.social_share_chats_row_name);
            time = (TextView)itemView.findViewById(R.id.social_share_chats_row_time);
        }
    }
}
