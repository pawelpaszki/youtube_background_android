/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pawelpaszki.youtubeplus.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

import java.util.List;

/**
 * Custom ArrayAdapter which enables setup of a list view row views
 * Created by smedic on 8.2.16..
 */
public class VideosAdapter extends RecyclerView.Adapter<VideosAdapter.ViewHolder> {

    private static final String TAG = "SMEDIC";
    private Context context;
    private final List<YouTubeVideo> list;
    private boolean[] itemChecked;
    private String mFragment;
    private ItemEventsListener<YouTubeVideo> itemEventsListener;

    public VideosAdapter(Context context, List<YouTubeVideo> list, String fragment) {
        super();
        this.list = list;
        this.context = context;
        this.itemChecked = new boolean[(int) Config.NUMBER_OF_VIDEOS_RETURNED];
        this.mFragment = fragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final YouTubeVideo video = list.get(position);
        Picasso.with(context).load(video.getThumbnailURL()).into(holder.thumbnail);

        holder.title.setText(video.getTitle());
        holder.title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Please choose option");
                String [] searchOptions = new String[] {"add to playlist", "download"};
                String [] recentOptions = new String[] {"add to playlist", "remove from the list", "download"};
                final String [] options;
                if(mFragment.equals("searchFragment")) {
                    options = searchOptions;
                } else {
                    options = recentOptions;
                }
                builder.setSingleChoiceItems(options, -1, new DialogInterface
                        .OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if(options[item].equals("add to playlist")) {
                            if (itemEventsListener != null) {
                                itemEventsListener.onAddClicked(video);
                            }
                        } else if (options[item].equals("remove from the list")) {
                            if (itemEventsListener != null) {
                                itemEventsListener.onRemoveClicked(video);
                            }
                        } else if (options[item].equals("download")) {
                            if (itemEventsListener != null) {
                                itemEventsListener.onDownloadClicked(video);
                            }
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
        Log.i("views", video.getViewCount());
        String views;
        if(video.getViewCount().length() < 10) {
            views = video.getViewCount();
        } else if (video.getViewCount().length() < 14) {
            String []tokens = video.getViewCount().split(",");
            views = tokens[0] + "K views";
        } else if (video.getViewCount().length() < 18){
            String []tokens = video.getViewCount().split(",");
            views = tokens[0] + "M views";
        } else {
            String []tokens = video.getViewCount().split(",");
            views = tokens[0] + "." + tokens[1] + "M views";
        }
        holder.viewCount.setText(views);
        holder.itemView.setTag(video);
    }

    @Override
    public int getItemCount() {
        return (null != list ? list.size() : 0);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView duration;
        TextView viewCount;

        public ViewHolder(View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.video_thumbnail);
            title = (TextView) itemView.findViewById(R.id.video_title);
            duration = (TextView) itemView.findViewById(R.id.video_duration);
            viewCount = (TextView) itemView.findViewById(R.id.views_number);
        }
    }

    public void setOnItemEventsListener(ItemEventsListener<YouTubeVideo> listener) {
        itemEventsListener = listener;
    }
}
