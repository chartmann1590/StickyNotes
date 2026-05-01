package com.charles.stickynotes;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.charles.stickynotes.databinding.ActivityMainBinding;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAnalytics firebaseAnalytics;
    private InterstitialAd interstitialAd;
    private NativeAd nativeAd;
    private final StickyNote note = new StickyNote();
    private int currentNoteIndex = 0;
    private ArrayAdapter<String> noteListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        setSupportActionBar(binding.toolbar);

        currentNoteIndex = note.getSelectedIndex(this);
        String requestedNoteId = getIntent().getStringExtra(NewAppWidget.EXTRA_NOTE_ID);
        if (!TextUtils.isEmpty(requestedNoteId)) {
            currentNoteIndex = note.getIndexByNoteId(this, requestedNoteId);
            note.setSelectedIndex(this, currentNoteIndex);
        }
        setupNotesList();
        refreshEditorFromSelected();
        startConsentFlow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        persistCurrentEditor();
        int id = item.getItemId();
        if (id == R.id.action_share) {
            shareNote();
            return true;
        }
        if (id == R.id.action_new_note) {
            currentNoteIndex = note.addNote(this);
            refreshEditorFromSelected();
            Toast.makeText(this, R.string.toast_note_created, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_previous_note) {
            moveToNote(-1);
            return true;
        }
        if (id == R.id.action_next_note) {
            moveToNote(1);
            return true;
        }
        if (id == R.id.action_delete_note) {
            currentNoteIndex = note.deleteCurrentNote(this);
            refreshEditorFromSelected();
            updateWidget();
            Toast.makeText(this, R.string.toast_note_deleted, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_clear) {
            binding.noteEditor.setText("");
            persistCurrentEditor();
            updateToolbarSubtitle();
            Toast.makeText(this, R.string.toast_cleared, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_privacy) {
            openInCustomTab(BuildConfig.PRIVACY_POLICY_URL);
            return true;
        }
        if (id == R.id.action_support) {
            openInCustomTab(BuildConfig.SUPPORT_URL);
            return true;
        }
        if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareNote() {
        String text = binding.noteEditor.getText() != null ? binding.noteEditor.getText().toString() : "";
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.menu_share)));
    }

    private void moveToNote(int delta) {
        int count = note.noteCount(this);
        if (count <= 1) {
            updateToolbarSubtitle();
            return;
        }
        int next = (currentNoteIndex + delta + count) % count;
        currentNoteIndex = next;
        note.setSelectedIndex(this, currentNoteIndex);
        refreshEditorFromSelected();
    }

    private void setupNotesList() {
        ListView listView = binding.notesList;
        noteListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, buildNoteRows());
        listView.setAdapter(noteListAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            persistCurrentEditor();
            currentNoteIndex = position;
            note.setSelectedIndex(this, currentNoteIndex);
            refreshEditorFromSelected();
        });
    }

    private java.util.List<String> buildNoteRows() {
        java.util.List<String> notes = note.getNotes(this);
        java.util.List<String> rows = new java.util.ArrayList<>();
        for (int i = 0; i < notes.size(); i++) {
            String txt = notes.get(i) == null ? "" : notes.get(i).trim();
            if (txt.isEmpty()) {
                txt = "(empty)";
            }
            if (txt.length() > 30) {
                txt = txt.substring(0, 30) + "...";
            }
            rows.add("Note " + (i + 1) + ": " + txt);
        }
        return rows;
    }

    private void refreshNotesList() {
        if (noteListAdapter == null) {
            return;
        }
        noteListAdapter.clear();
        noteListAdapter.addAll(buildNoteRows());
        noteListAdapter.notifyDataSetChanged();
    }

    private void refreshEditorFromSelected() {
        currentNoteIndex = note.getSelectedIndex(this);
        binding.noteEditor.setText(note.getCurrentNote(this));
        updateToolbarSubtitle();
        refreshNotesList();
    }

    private void updateToolbarSubtitle() {
        int count = note.noteCount(this);
        int selected = note.getSelectedIndex(this) + 1;
        binding.toolbar.setSubtitle(getString(R.string.toolbar_note_counter, selected, count));
    }

    private void persistCurrentEditor() {
        TextInputEditText editor = binding.noteEditor;
        String text = editor.getText() != null ? editor.getText().toString() : "";
        note.updateCurrentNote(this, text);
    }

    private void openInCustomTab(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(this, Uri.parse(url));
    }

    private void showAbout() {
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "?";
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_message, version))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void startConsentFlow() {
        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        this::onConsentFormDismissed
                ),
                requestConsentError -> onConsentFormDismissed(null)
        );
    }

    private void onConsentFormDismissed(@Nullable FormError formError) {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                runOnUiThread(MainActivity.this::loadAllAds);
            }
        });
    }

    private void loadAllAds() {
        loadBanner();
        loadNativeAd();
        loadInterstitial();
    }

    private void loadBanner() {
        AdView adView = binding.adView;
        adView.loadAd(new AdRequest.Builder().build());
    }

    private void loadNativeAd() {
        FrameLayout slot = binding.nativeAdContainer;
        AdLoader adLoader = new AdLoader.Builder(this, BuildConfig.ADMOB_NATIVE_AD_UNIT_ID)
                .forNativeAd(ad -> {
                    if (isFinishing() || isDestroyed()) {
                        ad.destroy();
                        return;
                    }
                    if (nativeAd != null) {
                        nativeAd.destroy();
                        nativeAd = null;
                    }
                    nativeAd = ad;
                    NativeAdView adView = (NativeAdView) getLayoutInflater()
                            .inflate(R.layout.layout_native_ad, slot, false);
                    slot.removeAllViews();
                    slot.addView(adView);
                    populateNativeAdView(ad, adView);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        // Leave slot empty on failure
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder().build())
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void populateNativeAdView(@NonNull NativeAd nativeAd, @NonNull NativeAdView adView) {
        adView.setMediaView(adView.findViewById(R.id.ad_media));
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }
        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }
        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }
        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
        }
        adView.setNativeAd(nativeAd);
    }

    private void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                    }
                }
        );
    }

    public void updateWidget() {
        NewAppWidget.updateAllWidgets(this);
    }

    public void saveButton(View v) {
        persistCurrentEditor();
        updateWidget();
        if (interstitialAd != null) {
            interstitialAd.show(this);
        }
        Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        persistCurrentEditor();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (binding != null) {
            binding.adView.destroy();
        }
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
        super.onDestroy();
    }
}
