package com.marverenic.music.data.store;

import android.content.Context;
import android.provider.MediaStore;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class LocalMusicStore implements MusicStore {

    private Context mContext;
    private PreferencesStore mPreferencesStore;

    private BehaviorSubject<List<Song>> mSongs;
    private BehaviorSubject<List<Album>> mAlbums;
    private BehaviorSubject<List<Artist>> mArtists;
    private BehaviorSubject<List<Genre>> mGenres;

    public LocalMusicStore(Context context, PreferencesStore preferencesStore) {
        mContext = context;
        mPreferencesStore = preferencesStore;
    }

    @Override
    public Observable<Boolean> refresh() {
        return MediaStoreUtil.promptPermission(mContext)
                .observeOn(Schedulers.io())
                .map(granted -> {
                    if (granted) {
                        if (mSongs != null) {
                            mSongs.onNext(getAllSongs());
                        }
                        if (mArtists != null) {
                            mArtists.onNext(getAllArtists());
                        }
                        if (mAlbums != null) {
                            mAlbums.onNext(getAllAlbums());
                        }
                        if (mGenres != null) {
                            mGenres.onNext(getAllGenres());
                        }
                    }
                    return granted;
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Song>> getSongs() {
        if (mSongs == null) {
            mSongs = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext)
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mSongs.onNext(getAllSongs());
                        } else {
                            mSongs.onNext(Collections.emptyList());
                        }
                    });
        }
        return mSongs.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Song> getAllSongs() {
        return MediaStoreUtil.getSongs(mContext, getDirectoryInclusionExclusionSelection(), null);
    }

    private String getDirectoryInclusionExclusionSelection() {
        String selection;

        String includeSelection = getDirectoryInclusionSelection();
        String excludeSelection = getDirectoryExclusionSelection();

        if (includeSelection != null && excludeSelection != null) {
            selection = "(" + includeSelection + ") AND (" + excludeSelection + ")";
        } else if (includeSelection != null) {
            selection = includeSelection;
        } else if (excludeSelection != null) {
            selection = excludeSelection;
        } else {
            selection = null;
        }

        return selection;
    }

    private String getDirectoryInclusionSelection() {
        if (mPreferencesStore.getIncludedDirectories().isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (String directory : mPreferencesStore.getIncludedDirectories()) {
            builder.append(MediaStore.Audio.Media.DATA)
                    .append(" LIKE \'")
                    .append(directory).append(File.separatorChar)
                    .append("%\'");

            builder.append(" OR ");
        }

        builder.setLength(builder.length() - 4);
        return builder.toString();
    }

    private String getDirectoryExclusionSelection() {
        if (mPreferencesStore.getExcludedDirectories().isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (String directory : mPreferencesStore.getExcludedDirectories()) {
            builder.append(MediaStore.Audio.Media.DATA)
                    .append(" NOT LIKE \'")
                    .append(directory).append(File.separatorChar)
                    .append("%\'");

            builder.append(" AND ");
        }

        builder.setLength(builder.length() - 5);
        return builder.toString();
    }

    @Override
    public Observable<List<Album>> getAlbums() {
        if (mAlbums == null) {
            mAlbums = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext)
                    .flatMap(granted -> {
                        if (noDirectoryFilters()) {
                            return Observable.just(granted);
                        } else {
                            return getSongs().map((List<Song> songs) -> granted);
                        }
                    })
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mAlbums.onNext(getAllAlbums());
                        } else {
                            mAlbums.onNext(Collections.emptyList());
                        }
                    });
        }
        return mAlbums.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Album> getAllAlbums() {
        return filterAlbums(MediaStoreUtil.getAlbums(mContext, null, null));
    }

    @Override
    public Observable<List<Artist>> getArtists() {
        if (mArtists == null) {
            mArtists = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext)
                    .flatMap(granted -> {
                        if (noDirectoryFilters()) {
                            return Observable.just(granted);
                        } else {
                            return getSongs().map((List<Song> songs) -> granted);
                        }
                    })
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mArtists.onNext(getAllArtists());
                        } else {
                            mArtists.onNext(Collections.emptyList());
                        }
                    });
        }
        return mArtists.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Artist> getAllArtists() {
        return filterArtists(MediaStoreUtil.getArtists(mContext, null, null));
    }

    @Override
    public Observable<List<Genre>> getGenres() {
        if (mGenres == null) {
            mGenres = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext)
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mGenres.onNext(getAllGenres());
                        } else {
                            mGenres.onNext(Collections.emptyList());
                        }
                    });
        }
        return mGenres.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Genre> getAllGenres() {
        return filterGenres(MediaStoreUtil.getGenres(mContext, null, null));
    }

    private boolean noDirectoryFilters() {
        boolean notIncludingFolders = mPreferencesStore.getIncludedDirectories().isEmpty();
        boolean notExcludingFolders = mPreferencesStore.getExcludedDirectories().isEmpty();

        return notExcludingFolders && notIncludingFolders;
    }

    private List<Album> filterAlbums(List<Album> albumsToFilter) {
        if (noDirectoryFilters()) {
            return albumsToFilter;
        }

        List<Album> filteredAlbums = new ArrayList<>();

        for (Album album : albumsToFilter) {
            for (Song song : mSongs.getValue()) {
                if (album.getAlbumId() == song.getAlbumId()) {
                    filteredAlbums.add(album);
                    break;
                }
            }
        }

        return filteredAlbums;
    }

    private List<Artist> filterArtists(List<Artist> artistsToFilter) {
        if (noDirectoryFilters()) {
            return artistsToFilter;
        }

        List<Artist> filteredArtists = new ArrayList<>();

        for (Artist artist : artistsToFilter) {
            for (Song song : mSongs.getValue()) {
                if (artist.getArtistId() == song.getArtistId()) {
                    filteredArtists.add(artist);
                    break;
                }
            }
        }

        return filteredArtists;
    }

    private List<Genre> filterGenres(List<Genre> genresToFilter) {
        if (noDirectoryFilters()) {
            return genresToFilter;
        }

        List<Genre> filteredGenres = new ArrayList<>();
        String directorySelection = getDirectoryInclusionExclusionSelection();

        for (Genre genre : genresToFilter) {
            boolean hasSongs = !MediaStoreUtil.getGenreSongs(mContext, genre,
                    directorySelection, null).isEmpty();

            if (hasSongs) {
                filteredGenres.add(genre);
            }
        }

        return filteredGenres;
    }

    @Override
    public Observable<List<Song>> getSongs(Artist artist) {
        String selection = MediaStore.Audio.Media.ARTIST_ID + " = ?";
        String[] selectionArgs = {Long.toString(artist.getArtistId())};

        String directorySelection = getDirectoryInclusionExclusionSelection();
        if (directorySelection != null) {
            selection += " AND " + directorySelection;
        }

        return Observable.just(MediaStoreUtil.getSongs(mContext, selection, selectionArgs));
    }

    @Override
    public Observable<List<Song>> getSongs(Album album) {
        String selection = MediaStore.Audio.Media.ALBUM_ID + " = ? ";
        String[] selectionArgs = {Long.toString(album.getAlbumId())};

        String directorySelection = getDirectoryInclusionExclusionSelection();
        if (directorySelection != null) {
            selection += " AND " + directorySelection;
        }

        return Observable.just(MediaStoreUtil.getSongs(mContext, selection, selectionArgs));
    }

    @Override
    public Observable<List<Song>> getSongs(Genre genre) {
        return Observable.just(MediaStoreUtil.getGenreSongs(mContext, genre,
                getDirectoryInclusionExclusionSelection(), null));
    }

    @Override
    public Observable<List<Album>> getAlbums(Artist artist) {
        return Observable.just(filterAlbums(MediaStoreUtil.getArtistAlbums(mContext, artist)));
    }

    @Override
    public Observable<Artist> findArtistById(long artistId) {
        return Observable.just(MediaStoreUtil.findArtistById(mContext, artistId));
    }

    @Override
    public Observable<Album> findAlbumById(long albumId) {
        return Observable.just(MediaStoreUtil.findAlbumById(mContext, albumId));
    }

    @Override
    public Observable<Artist> findArtistByName(String artistName) {
        return Observable.just(MediaStoreUtil.findArtistByName(mContext, artistName));
    }

    @Override
    public Observable<List<Song>> searchForSongs(String query) {
        return Observable.just(MediaStoreUtil.searchForSongs(mContext, query));
    }

    @Override
    public Observable<List<Artist>> searchForArtists(String query) {
        return Observable.just(MediaStoreUtil.searchForArtists(mContext, query));
    }

    @Override
    public Observable<List<Album>> searchForAlbums(String query) {
        return Observable.just(MediaStoreUtil.searchForAlbums(mContext, query));
    }

    @Override
    public Observable<List<Genre>> searchForGenres(String query) {
        return Observable.just(MediaStoreUtil.searchForGenres(mContext, query));
    }
}
