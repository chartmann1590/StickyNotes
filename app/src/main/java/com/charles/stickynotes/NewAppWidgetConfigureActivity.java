package com.charles.stickynotes;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class NewAppWidgetConfigureActivity extends Activity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private StickyNote noteRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_widget_configure);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View root = findViewById(R.id.widget_config_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left + 16, bars.top + 16, bars.right + 16, bars.bottom + 16);
            return insets;
        });

        noteRepo = new StickyNote();
        appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        );
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        ListView listView = findViewById(R.id.note_list_view);
        List<String> display = buildDisplayList();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                display
        );
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String noteId = noteRepo.getNoteIdAt(this, position);
            WidgetNoteLinkStore.setNoteIdForWidget(this, appWidgetId, noteId);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            NewAppWidget.updateWidget(this, manager, appWidgetId);

            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private List<String> buildDisplayList() {
        List<String> notes = noteRepo.getNotes(this);
        List<String> output = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++) {
            String text = notes.get(i);
            text = text == null ? "" : text.trim();
            if (text.isEmpty()) {
                text = "(empty)";
            }
            if (text.length() > 40) {
                text = text.substring(0, 40) + "...";
            }
            output.add("Note " + (i + 1) + ": " + text);
        }
        return output;
    }
}
