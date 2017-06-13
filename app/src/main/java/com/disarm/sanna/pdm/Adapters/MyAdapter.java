package com.disarm.sanna.pdm.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.disarm.sanna.pdm.R;

/**
 * Created by Sanna on 20-06-2016.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    int[] nameContainer;
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_list_view_main_act, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.title.setText(nameContainer[position]);
    }

    @Override
    public int getItemCount() {
        return nameContainer.length;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder  {
        public TextView title;
        public MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
        }
    }
    public MyAdapter(int[] nameResult) {
        nameContainer = nameResult;
    }
}
