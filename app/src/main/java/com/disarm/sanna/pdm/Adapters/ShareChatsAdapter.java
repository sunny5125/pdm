package com.disarm.sanna.pdm.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.disarm.sanna.pdm.R;
import com.disarm.sanna.pdm.SocialShareActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by arka on 23/9/16.
 */

public class ShareChatsAdapter extends BaseAdapter {
    private static final String TAG = "ShareChatsAdapter";

    private static final int TYPE_TEXT = 0;
    private static final int TYPE_THUMBNAIL = 1;
    private static final int TYPE_MAX_COUNT = 2;

    private ArrayList<String> msg;
    private Context context;
    private String sender;
    private ShareChatsInterface anInterface;

    public ShareChatsAdapter(ArrayList<String> msg, String sender, Context context) {
        this.msg = msg;
        this.sender = sender;
        this.context = context;
    }

    @Override
    public int getCount() {
        return msg.size();
    }

    @Override
    public Object getItem(int position) {
        return msg.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        if(msg.get(position).startsWith("IMG") || msg.get(position).startsWith("VID")) {
            return TYPE_THUMBNAIL;
        }
        return TYPE_TEXT;
    }

    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {
        View rowView = view;
        int type = getItemViewType(position);

        switch (type) {
            case TYPE_TEXT:
                ViewHolder holder;

                if (rowView == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE);
                    rowView = inflater.inflate(R.layout.share_activity_row, viewGroup, false);
                    holder = new ViewHolder(rowView);
                    rowView.setTag(holder);
                } else {
                    holder = (ViewHolder) rowView.getTag();
                }

                holder.msgText.setText(msg.get(position));
                break;
            case TYPE_THUMBNAIL:
                ViewHolderThumbnail holderThumbnail;

                if(rowView == null) {
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE);
                    rowView = inflater.inflate(R.layout.share_activity_row_thumbnail,
                            viewGroup, false);
                    holderThumbnail = new ViewHolderThumbnail(rowView);
                    rowView.setTag(holderThumbnail);
                } else {
                    holderThumbnail = (ViewHolderThumbnail) rowView.getTag();
                }

                Bitmap thumbnail = null;
                if(msg.get(position).startsWith("IMG")) {
                    thumbnail = getThumbnail(Environment.getExternalStorageDirectory().toString() +
                            SocialShareActivity.WORKING_DIRECTORY + msg.get(position));
                } else if(msg.get(position).startsWith("VID")) {
                    thumbnail = getThumbnailOfVideo(
                            Environment.getExternalStorageDirectory().toString() +
                            SocialShareActivity.WORKING_DIRECTORY + msg.get(position));
                }
                holderThumbnail.msgThumbnail.setImageBitmap(thumbnail);
                break;
        }

        LinearLayout layout = (LinearLayout)rowView.findViewById(R.id.share_bubble_layout);
        LinearLayout layoutParent = (LinearLayout)rowView.findViewById(
                R.id.share_bubble_layout_parent);

        if(msg.get(position).split("_")[3].equals(sender)) {
            layout.setBackgroundResource(R.drawable.bubble1);
            layoutParent.setGravity(Gravity.LEFT);
        } else {
            layout.setBackgroundResource(R.drawable.bubble2);
            layoutParent.setGravity(Gravity.RIGHT);
        }

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(anInterface != null) {
                    anInterface.onClick(view, position);
                }
            }
        });

        return rowView;
    }

    /**
     * Get thumbnail of an image from its path
     * @param path
     * @return
     */
    private Bitmap getThumbnail(String path) {
        Bitmap imgThumbnail = null;
        try {
            final int THUMBNAIL_SIZE = 128;
            imgThumbnail = decodeSampledBitmapFromResource(path, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG + " ERROR", "FILE NOT FOUND AT : " + path);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG + " ERROR", "IOException : Buffered Input Stream");
        }
        return imgThumbnail;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            /*
            Calculate the largest inSampleSize value that is a power of 2 and keeps both
            height and width larger than the requested height and width.
            */
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        Log.d(TAG + "BITMAP", " Sample Size " + inSampleSize);
        return inSampleSize;
    }

    private static Bitmap decodeSampledBitmapFromResource(String path, int reqWidth,
                                                          int reqHeight) throws IOException {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new FileInputStream(path), null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(new FileInputStream(path), null, options);
    }

    private Bitmap getThumbnailOfVideo(String path) {
        return ThumbnailUtils.createVideoThumbnail(path,
                MediaStore.Video.Thumbnails.MINI_KIND);
    }

    private class ViewHolder {
        private TextView msgText;

        private ViewHolder(View item) {
            msgText = (TextView)item.findViewById(R.id.share_row_message_text);
        }
    }

    private class ViewHolderThumbnail {
        private ImageView msgThumbnail;

        private ViewHolderThumbnail(View item) {
            msgThumbnail = (ImageView)item.findViewById(R.id.share_row_message_thumbnail);
        }
    }

    public void setAnInterface(ShareChatsInterface anInterface) {
        this.anInterface = anInterface;
    }

    public interface ShareChatsInterface {
        void onClick(View row, int position);
    }
}
