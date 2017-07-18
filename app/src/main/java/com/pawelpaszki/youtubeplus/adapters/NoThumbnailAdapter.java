package com.pawelpaszki.youtubeplus.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;

import java.io.File;
import java.util.List;

/**
 * Created by PawelPaszki on 07/07/2017.
 * This adapter creates playlist items without the thumbnail
 */

public class NoThumbnailAdapter extends RecyclerView.Adapter<NoThumbnailAdapter.ViewHolder>
         {

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
        holder.title.setText(video.getTitle());
        holder.title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Please choose option");
                String [] downloadsOptions = new String[] {"add to playlist", "remove from the list"};
                String [] playlistsOptions = new String[] {"add to playlist", "remove from the list", "download"};
                final String [] options;
                if(mFragment.equals("downloadedFragment")) {
                    options = downloadsOptions;
                } else {
                    options = playlistsOptions;
                }
                builder.setSingleChoiceItems(options, -1, new DialogInterface
                        .OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch (options[item]) {
                            case "add to playlist":
                                if (itemEventsListener != null) {
                                    itemEventsListener.onAddClicked(video);
                                }
                                break;
                            case "remove from the list":
                                if (itemEventsListener != null) {
                                    itemEventsListener.onRemoveClicked(video);
                                }
                                break;
                            case "download":
                                if (itemEventsListener != null) {
                                    itemEventsListener.onDownloadClicked(video);
                                }
                                break;
                        }
                        dialog.dismiss();

                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                Log.i("long click", "true");
                return true;
            }

        });
        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemEventsListener != null) {
                    itemEventsListener.onItemClick(video);
                }
            }
        });
        holder.duration.setText(video.getDuration());
        holder.itemView.setTag(video);
    }

    @Override
    public int getItemCount() {
        return (null != list ? list.size() : 0);
    }

     class ViewHolder extends RecyclerView.ViewHolder{
        TextView title;
        TextView duration;

        ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.item_title);
            duration = (TextView) itemView.findViewById(R.id.item_duration);
        }
    }

    public void setOnItemEventsListener(ItemEventsListener<YouTubeVideo> listener) {
        itemEventsListener = listener;
    }

}
