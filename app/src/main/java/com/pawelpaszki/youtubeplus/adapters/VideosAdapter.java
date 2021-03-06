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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Custom ArrayAdapter which enables setup of a list view row views
 * Created by smedic on 8.2.16..
 */
public class VideosAdapter extends RecyclerView.Adapter<VideosAdapter.ViewHolder>  implements View.OnClickListener, View.OnLongClickListener{

    private Context context;
    private final List<YouTubeVideo> list;
    private String mFragment;
    private ItemEventsListener<YouTubeVideo> itemEventsListener;

    public VideosAdapter(Context context, List<YouTubeVideo> list, String fragment) {
        super();
        this.list = list;
        this.context = context;
        this.mFragment = fragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item, null);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final YouTubeVideo video = list.get(position);
        Picasso.with(context).load(video.getThumbnailURL()).into(holder.thumbnail);

        holder.title.setText(video.getTitle());holder.duration.setText(video.getDuration());
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

    @Override
    public void onClick(View v) {
        if (itemEventsListener != null) {
            YouTubeVideo item = (YouTubeVideo) v.getTag();
            itemEventsListener.onItemClick(item);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (itemEventsListener != null) {
            final YouTubeVideo video = (YouTubeVideo) v.getTag();
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Please choose option");
            final String[] options = new String[]{"download audio"};

            builder.setSingleChoiceItems(options, -1, new DialogInterface
                    .OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (options[item]) {
                        case "download audio":
                            if (itemEventsListener != null) {
                                itemEventsListener.onDownloadClicked(video, Config.MediaType.AUDIO);
                            }
                            break;
                    }
                    dialog.dismiss();

                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        return true;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView duration;
        TextView viewCount;

        ViewHolder(View itemView) {
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
