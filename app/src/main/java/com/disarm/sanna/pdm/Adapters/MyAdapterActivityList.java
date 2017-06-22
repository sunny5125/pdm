package com.disarm.sanna.pdm.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.disarm.sanna.pdm.R;

/**
 * Created by Sanna on 22-06-2016.
 */
public class MyAdapterActivityList extends RecyclerView.Adapter<MyAdapterActivityList.MyViewHolder> {
    int[] imgConatiner;
    int[] nameContainer;
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_list_view_activity_list, parent, false);

        return new MyViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.head.setText(nameContainer[position]);
        holder.count.setText(R.string.msg_taken);
        holder.img.setImageResource(imgConatiner[position]);
    }


    @Override
    public int getItemCount() {
        return nameContainer.length;
    }

    public class MyViewHolder  extends RecyclerView.ViewHolder{
        public TextView head,count;
        public ImageView img;
        public MyViewHolder(View itemView) {
            super(itemView);
            head = (TextView)itemView.findViewById(R.id.activity_list_textView_title);
            count = (TextView)itemView.findViewById(R.id.activity_list_textView_count);
           img = (ImageView)itemView.findViewById(R.id.activit_list_imgView);

        }
    }
    public MyAdapterActivityList(int[] nameResult, int[] imgResult){
        nameContainer = nameResult;
        imgConatiner = imgResult;
    }
}
