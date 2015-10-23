package com.marverenic.music.instances.viewholder;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.marverenic.music.Library;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.PlaylistDialog;
import com.marverenic.music.view.ViewUtils;

import java.util.HashMap;

public class AlbumViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        Palette.PaletteAsyncListener, PopupMenu.OnMenuItemClickListener,
        RequestListener<String, GlideDrawable>{

    // Used to cache Palette values in memory
    private static HashMap<Album, int[]> colorCache = new HashMap<>();
    private static final int FRAME_COLOR = 0;
    private static final int TITLE_COLOR = 1;
    private static final int DETAIL_COLOR = 2;

    private static int defaultFrameColor;
    private static int defaultTitleColor;
    private static int defaultDetailColor;

    private View itemView;
    private FrameLayout container;
    private TextView albumName;
    private TextView artistName;
    private ImageView artwork;
    private Album reference;

    private AsyncTask<Bitmap, Void, Palette> paletteTask;
    private ObjectAnimator backgroundAnimator;
    private ObjectAnimator titleAnimator;
    private ObjectAnimator detailAnimator;

    @SuppressWarnings("deprecation")
    public AlbumViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;

        defaultFrameColor = itemView.getResources().getColor(R.color.grid_background_default);
        defaultTitleColor = itemView.getResources().getColor(R.color.grid_text);
        defaultDetailColor = itemView.getResources().getColor(R.color.grid_detail_text);

        container = (FrameLayout) itemView;
        albumName = (TextView) itemView.findViewById(R.id.instanceTitle);
        artistName = (TextView) itemView.findViewById(R.id.instanceDetail);
        ImageView moreButton = (ImageView) itemView.findViewById(R.id.instanceMore);
        artwork = (ImageView) itemView.findViewById(R.id.instanceArt);

        itemView.setOnClickListener(this);
        moreButton.setOnClickListener(this);
    }

    public void update(Album a){
        if (paletteTask != null && !paletteTask.isCancelled()) paletteTask.cancel(true);

        reference = a;
        albumName.setText(a.albumName);
        artistName.setText(a.artistName);

        resetPalette();

        Glide.with(itemView.getContext())
                .load("file://" + a.artUri)
                .placeholder(R.drawable.art_default)
                .animate(android.R.anim.fade_in)
                .crossFade()
                .listener(this)
                .into(artwork);
    }

    @Override
    public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                               boolean isFirstResource) {
        return false;
    }

    @Override
    public boolean onResourceReady(GlideDrawable resource, String model,
                                   Target<GlideDrawable> target, boolean isFromMemoryCache,
                                   boolean isFirstResource) {
        if (isFromMemoryCache) {
            updatePalette(resource);
        } else {
            animatePalette(resource);
        }
        return false;
    }

    private void generatePalette(Drawable drawable) {
        if (colorCache.get(reference) == null) {
            paletteTask = Palette.from(ViewUtils.drawableToBitmap(drawable)).generate(this);
        }
    }

    private void resetPalette() {
        if (paletteTask != null && !paletteTask.isCancelled()) paletteTask.cancel(true);

        if (backgroundAnimator != null){
            backgroundAnimator.setDuration(0);
            backgroundAnimator.cancel();
        }
        if (titleAnimator != null){
            titleAnimator.setDuration(0);
            titleAnimator.cancel();
        }
        if (detailAnimator != null){
            detailAnimator.setDuration(0);
            detailAnimator.cancel();
        }

        container.setBackgroundColor(defaultFrameColor);
        albumName.setTextColor(defaultTitleColor);
        artistName.setTextColor(defaultDetailColor);
    }

    private void updatePalette(Drawable drawable) {
        int[] colors = colorCache.get(reference);

        if (colors != null) {
            container.setBackgroundColor(colors[FRAME_COLOR]);
            albumName.setTextColor(colors[TITLE_COLOR]);
            artistName.setTextColor(colors[DETAIL_COLOR]);
        } else {
            resetPalette();
            generatePalette(drawable);
        }
    }

    private void animatePalette(Drawable drawable) {
        int[] colors = colorCache.get(reference);

        if (colors != null) {
            backgroundAnimator = ObjectAnimator.ofObject(
                    container,
                    "backgroundColor",
                    new ArgbEvaluator(),
                    defaultFrameColor,
                    colors[FRAME_COLOR]);
            backgroundAnimator.setDuration(300).start();

            titleAnimator = ObjectAnimator.ofObject(
                    albumName,
                    "textColor",
                    new ArgbEvaluator(),
                    defaultTitleColor,
                    colors[TITLE_COLOR]);
            titleAnimator.setDuration(300).start();

            detailAnimator = ObjectAnimator.ofObject(
                    artistName,
                    "textColor",
                    new ArgbEvaluator(),
                    defaultDetailColor,
                    colors[DETAIL_COLOR]);
            detailAnimator.setDuration(300).start();
        } else {
            generatePalette(drawable);
        }
    }

    @Override
    public void onGenerated(Palette palette) {
        int frameColor = defaultFrameColor;
        int titleColor = defaultTitleColor;
        int detailColor = defaultDetailColor;

        if (palette.getVibrantSwatch() != null && palette.getVibrantColor(-1) != -1) {
            frameColor = palette.getVibrantColor(0);
            titleColor = palette.getVibrantSwatch().getTitleTextColor();
            detailColor = palette.getVibrantSwatch().getBodyTextColor();
        } else if (palette.getLightVibrantSwatch() != null && palette.getLightVibrantColor(-1) != -1) {
            frameColor = palette.getLightVibrantColor(0);
            titleColor = palette.getLightVibrantSwatch().getTitleTextColor();
            detailColor = palette.getLightVibrantSwatch().getBodyTextColor();
        } else if (palette.getDarkVibrantSwatch() != null && palette.getDarkVibrantColor(-1) != -1) {
            frameColor = palette.getDarkVibrantColor(0);
            titleColor = palette.getDarkVibrantSwatch().getTitleTextColor();
            detailColor = palette.getDarkVibrantSwatch().getBodyTextColor();
        } else if (palette.getLightMutedSwatch() != null && palette.getLightMutedColor(-1) != -1) {
            frameColor = palette.getLightMutedColor(0);
            titleColor = palette.getLightMutedSwatch().getTitleTextColor();
            detailColor = palette.getLightMutedSwatch().getBodyTextColor();
        } else if (palette.getDarkMutedSwatch() != null && palette.getDarkMutedColor(-1) != -1) {
            frameColor = palette.getDarkMutedColor(0);
            titleColor = palette.getDarkMutedSwatch().getTitleTextColor();
            detailColor = palette.getDarkMutedSwatch().getBodyTextColor();
        }

        colorCache.put(reference, new int[]{frameColor, titleColor, detailColor});
        animatePalette(null);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.instanceMore:
                final PopupMenu menu = new PopupMenu(itemView.getContext(), v, Gravity.END);
                String[] options = itemView.getResources().getStringArray(R.array.queue_options_album);
                for (int i = 0; i < options.length; i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
                break;
            default:
                Navigate.to(itemView.getContext(), AlbumActivity.class, AlbumActivity.ALBUM_EXTRA, reference);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 0: //Queue this album next
                PlayerController.queueNext(Library.getAlbumEntries(reference));
                return true;
            case 1: //Queue this album last
                PlayerController.queueLast(Library.getAlbumEntries(reference));
                return true;
            case 2: //Go to artist
                Navigate.to(
                        itemView.getContext(),
                        ArtistActivity.class,
                        ArtistActivity.ARTIST_EXTRA,
                        Library.findArtistById(reference.artistId));
                return true;
            case 3: //Add to playlist...
                PlaylistDialog.AddToNormal.alert(
                        itemView,
                        Library.getAlbumEntries(reference),
                        itemView.getContext()
                                .getString(R.string.header_add_song_name_to_playlist, reference));
                return true;
        }
        return false;
    }
}