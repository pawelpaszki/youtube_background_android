package com.pawelpaszki.youtubeplus.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.database.YouTubeSqlDb;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

/**
 * Created by PawelPaszki on 07/07/2017.
 */

public class NoThumbnailAdapter extends RecyclerView.Adapter<NoThumbnailAdapter.ViewHolder>
         {

    private static final String TAG = "SMEDIC";
    private Context context;
    private final List<YouTubeVideo> list;
    private String mFragment;
    private ItemEventsListener<YouTubeVideo> itemEventsListener;

    public NoThumbnailAdapter(Context context, List<YouTubeVideo> list, String fragment) {
        super();
        this.list = list;
        this.context = context;
        this.mFragment = fragment;
    }

    @Override
    public NoThumbnailAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.no_thumbnail_item, null);

        return new NoThumbnailAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoThumbnailAdapter.ViewHolder holder, final int position) {
        final YouTubeVideo video = list.get(position);

        holder.removeButton.bringToFront();
        holder.removeButton.setClickable(true);
        holder.addButton.bringToFront();
        holder.addButton.setClickable(true);
        holder.addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
                    itemEventsListener.onAddClicked(video);
                }
            }
        });
        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
                    itemEventsListener.onRemoveClicked(video);
                }
            }
        });
        holder.title.setText(video.getTitle());
        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
                    itemEventsListener.onItemClick(video);
                }
            }
        });
        if(mFragment.equals("downloadedFragment")) {
            holder.additionalItem.setVisibility(View.GONE);
        } else {
            holder.additionalItem.bringToFront();
            holder.additionalItem.setClickable(true);
            Resources res = context.getResources();
            Drawable audio = res.getDrawable(R.drawable.download);
            holder.additionalItem.setBackground(audio);
            holder.additionalItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemEventsListener != null) {
                        itemEventsListener.onDownloadClicked(video);
                    }
                }
            });
        }
        holder.duration.setText(video.getDuration());
        holder.itemView.setTag(video);
    }

    @Override
    public int getItemCount() {
        return (null != list ? list.size() : 0);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView duration;
        ImageView addButton;
        ImageView additionalItem;
        ImageView removeButton;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.item_title);
            duration = (TextView) itemView.findViewById(R.id.item_duration);
            addButton = (ImageView) itemView.findViewById(R.id.add_button);
            additionalItem = (ImageView) itemView.findViewById(R.id.additional_item);
            removeButton = (ImageView) itemView.findViewById(R.id.remove_button);
        }
    }

    public void setOnItemEventsListener(ItemEventsListener<YouTubeVideo> listener) {
        itemEventsListener = listener;
    }

     private String checkMediaType(YouTubeVideo video) {
         String filename = video.getId();
         File[] files = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles();
         for (File file : files) {
             if (file.getAbsolutePath().contains(filename)) {
                 if(file.getAbsolutePath().endsWith(".mp4")) {
                     return "video";
                 } else if (file.getAbsolutePath().endsWith(".mp3")) {
                     return "audio";
                 }
             }
         }
         return null;
     }
}
