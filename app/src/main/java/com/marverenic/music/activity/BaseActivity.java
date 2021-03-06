package com.marverenic.music.activity;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.annotations.PresetTheme;
import com.marverenic.music.data.store.PreferencesStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import javax.inject.Inject;

public abstract class BaseActivity extends RxAppCompatActivity
        implements PlayerController.UpdateListener, PlayerController.ErrorListener {

    private static final boolean DEBUG = BuildConfig.DEBUG;

    // Used when resuming the Activity to respond to a potential theme change
    @PresetTheme
    private int mTheme;
    private boolean mIsDark;

    @Inject PreferencesStore mPreferencesStore;
    @Inject ThemeStore mThemeStore;

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.i(getClass().toString(), "Called onCreate");
        JockeyApplication.getComponent(this).injectBaseActivity(this);

        mThemeStore.setTheme(this);
        mIsDark = getResources().getBoolean(R.bool.night);
        mTheme = mPreferencesStore.getPrimaryColor();

        PlayerController.startService(getApplicationContext());

        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (mPreferencesStore.showFirstStart()) {
            showFirstRunDialog();
        }
    }

    private void showFirstRunDialog() {
        View messageView = getLayoutInflater().inflate(R.layout.alert_pref, null);
        TextView message = (TextView) messageView.findViewById(R.id.alertMessage);
        CheckBox pref = (CheckBox) messageView.findViewById(R.id.alertPref);

        message.setText(Html.fromHtml(getString(R.string.first_launch_detail)));
        message.setMovementMethod(LinkMovementMethod.getInstance());

        pref.setChecked(true);
        pref.setText(R.string.enable_additional_logging_detailed);

        new AlertDialog.Builder(this)
                .setTitle(R.string.first_launch_title)
                .setView(messageView)
                .setPositiveButton(R.string.action_agree,
                        (dialog, which) -> {
                            mPreferencesStore.setAllowLogging(pref.isChecked());
                            mPreferencesStore.setShowFirstStart(false);
                        })
                .setCancelable(false)
                .show();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        super.setContentView(layoutResId);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.i(getClass().toString(), "Called onResume");

        // If the theme was changed since this Activity was created, or the automatic day/night
        // theme has changed state, recreate this activity
        mThemeStore.setTheme(this);
        boolean themeChanged = mTheme != mPreferencesStore.getPrimaryColor();
        boolean nightChanged = mIsDark != getResources().getBoolean(R.bool.night);

        if (themeChanged || nightChanged) {
            recreate();
        } else {
            PlayerController.registerUpdateListener(this);
            PlayerController.registerErrorListener(this);
            onUpdate();
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.i(getClass().toString(), "Called onPause");
        PlayerController.unregisterUpdateListener(this);
        PlayerController.unregisterErrorListener(this);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.i(getClass().toString(), "Called onDestroy");
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Navigate.up(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onBackPressed() {
        if (DEBUG) Log.i(getClass().toString(), "Called calledOnBackPressed");
        super.onBackPressed();
        Navigate.back(this);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onUpdate() {
        if (DEBUG) Log.i(getClass().toString(), "Called onUpdate");
    }

    @Override
    public void onError(String message) {
        if (DEBUG) Log.i(getClass().toString(), "Called onError : " + message);
        showSnackbar(message);
    }

    protected void showSnackbar(String message) {
        View content = findViewById(R.id.list);
        if (content == null) {
            content = findViewById(android.R.id.content);
        }
        Snackbar.make(content, message, Snackbar.LENGTH_LONG).show();
    }
}
