package com.charles.stickynotes;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

// Implementation of App Widget functionality
public class NewAppWidget extends AppWidgetProvider {
    public static final String EXTRA_NOTE_ID = "com.charles.stickynotes.extra.NOTE_ID";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            WidgetNoteLinkStore.removeWidget(context, appWidgetId);
        }
    }

    static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new android.content.ComponentName(context, NewAppWidget.class));
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        StickyNote repo = new StickyNote();
        String noteId = WidgetNoteLinkStore.getNoteIdForWidget(context, appWidgetId);
        if (noteId == null) {
            noteId = repo.getSelectedNoteId(context);
            WidgetNoteLinkStore.setNoteIdForWidget(context, appWidgetId, noteId);
        }

        Intent launchActivity = new Intent(context, MainActivity.class);
        launchActivity.putExtra(EXTRA_NOTE_ID, noteId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, launchActivity, flags);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        remoteViews.setTextViewText(R.id.appwidget_text, repo.getNoteTextById(context, noteId));
        remoteViews.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
}
