package com.charles.stickynotes;

import android.content.Context;
import android.content.SharedPreferences;

class WidgetNoteLinkStore {
    private static final String PREFS = "widget_note_links";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void setNoteIdForWidget(Context context, int appWidgetId, String noteId) {
        prefs(context).edit().putString(String.valueOf(appWidgetId), noteId).apply();
    }

    static String getNoteIdForWidget(Context context, int appWidgetId) {
        return prefs(context).getString(String.valueOf(appWidgetId), null);
    }

    static void removeWidget(Context context, int appWidgetId) {
        prefs(context).edit().remove(String.valueOf(appWidgetId)).apply();
    }
}
