package com.pawelpaszki.youtubeplus.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pawelpaszki.youtubeplus.MainActivity;
import com.pawelpaszki.youtubeplus.R;
import com.pawelpaszki.youtubeplus.interfaces.ItemEventsListener;
import com.pawelpaszki.youtubeplus.interfaces.ItemTouchHelperAdapter;
import com.pawelpaszki.youtubeplus.model.YouTubeVideo;
import com.pawelpaszki.youtubeplus.utils.Config;

import java.util.ArrayList;
import java.util.List;

import static com.pawelpaszki.youtubeplus.adapters.SimpleItemTouchHelperCallback.isLongPressEnabled;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_DOWNLOADED_LIST_REARRANGED;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_PLAYLISTS_REARRANGED;
import static com.pawelpaszki.youtubeplus.utils.Config.ACTION_VIDEO_UPDATE;

/**
 * Created by PawelPaszki on 07/07/2017.
 * This adapter creates playlist items without the thumbnail
 */

public class NoThumbnailAdapter extends RecyclerView.Adapter<NoThumbnailAdapter.ViewHolder> implements ItemTouchHelperAdapter
         {

    private Context context;

     public ArrayList<YouTubeVideo> getVideoList() {
         return videoList;
     }

     public ArrayList<YouTubeVideo> getIds() {
         return ids;
     }

     public ArrayList<YouTubeVideo> ids = new ArrayList<>();



     private ArrayList<YouTubeVideo> videoList;
    private String mFragment;
    public static boolean downloadedRearranged;
    public static boolean playListRearranged;
    private ItemEventsListener<YouTubeVideo> itemEventsListener;

    public NoThumbnailAdapter(Context context, List<YouTubeVideo> list, String fragment) {
        super();
        this.videoList = (ArrayList<YouTubeVideo>) list;
        ids = (ArrayList<YouTubeVideo>) list;
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
        final YouTubeVideo video = videoList.get(position);
        holder.title.setText(video.getTitle());
        holder.extras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Please choose option");
                String [] downloadsOptions = new String[] {"add to playlist", "remove from the list"};
                String [] playlistsOptions = new String[] {"add to playlist", "remove from the list", "download video", "download audio"};
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
                            case "download video":
                                if (itemEventsListener != null) {
                                    itemEventsListener.onDownloadClicked(video, Config.MediaType.VIDEO);
                                }
                                break;
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
        holder.itemView.setTag(video.getId());
    }

    @Override
    public int getItemCount() {
        return (null != videoList ? videoList.size() : 0);
    }

     @Override
     public void onItemMove(int fromPosition, int toPosition) {
         if(isLongPressEnabled()) {
             if(MainActivity.currentFragment == 0) {
                 downloadedRearranged = true;
                 Intent new_intent = new Intent();
                 new_intent.setAction(ACTION_DOWNLOADED_LIST_REARRANGED);
                 context.sendBroadcast(new_intent);
             } else if (MainActivity.currentFragment == 1) {
                 playListRearranged = true;
                 Intent new_intent = new Intent();
                 new_intent.setAction(ACTION_PLAYLISTS_REARRANGED);
                 context.sendBroadcast(new_intent);
             }
             ids = swapArrayItems(ids, fromPosition, toPosition);
             videoList = swapArrayItems(ids, fromPosition, toPosition);
             notifyItemMoved(fromPosition, toPosition);
         }
     }

     private ArrayList<YouTubeVideo> swapArrayItems(ArrayList<YouTubeVideo> list, int fromPosition, int toPosition) {
         ArrayList<YouTubeVideo> newList = new ArrayList<>();
         newList.addAll(list);
         newList.set(fromPosition, list.get(toPosition));
         newList.set(toPosition, list.get(fromPosition));
         return newList;
     }


     class ViewHolder extends RecyclerView.ViewHolder{

        TextView title;
        TextView duration;
        ImageView extras;

        ViewHolder(View itemView) {
            super(itemView);
            extras = (ImageView) itemView.findViewById(R.id.extra_options_iv);
            title = (TextView) itemView.findViewById(R.id.item_title);
            duration = (TextView) itemView.findViewById(R.id.item_duration);
        }
    }

    public void setOnItemEventsListener(ItemEventsListener<YouTubeVideo> listener) {
        itemEventsListener = listener;
    }

}
