package com.charles.stickynotes;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StickyNote {
    private static final String PREFS = "sticky_notes_prefs";
    private static final String KEY_NOTES = "notes_json";
    private static final String KEY_SELECTED_INDEX = "selected_index";

    static class NoteItem {
        final String id;
        final String text;

        NoteItem(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    private SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private List<NoteItem> ensureNotes(Context context) {
        List<NoteItem> notes = getNoteItems(context);
        if (notes.isEmpty()) {
            notes.add(new NoteItem(UUID.randomUUID().toString(), ""));
            saveNoteItems(context, notes);
            setSelectedIndex(context, 0);
        }
        return notes;
    }

    private List<NoteItem> getNoteItems(Context context) {
        String raw = prefs(context).getString(KEY_NOTES, "[]");
        List<NoteItem> notes = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                Object value = arr.opt(i);
                if (value instanceof JSONObject) {
                    JSONObject obj = (JSONObject) value;
                    String id = obj.optString("id", UUID.randomUUID().toString());
                    String text = obj.optString("text", "");
                    notes.add(new NoteItem(id, text));
                } else {
                    // Migration path from old string-array format.
                    notes.add(new NoteItem(UUID.randomUUID().toString(), arr.optString(i, "")));
                }
            }
        } catch (JSONException ignored) {
            // Reset malformed data.
        }
        return notes;
    }

    private void saveNoteItems(Context context, List<NoteItem> notes) {
        JSONArray arr = new JSONArray();
        for (NoteItem note : notes) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", note.id);
                obj.put("text", note.text == null ? "" : note.text);
                arr.put(obj);
            } catch (JSONException ignored) {
            }
        }
        prefs(context).edit().putString(KEY_NOTES, arr.toString()).apply();
    }

    List<String> getNotes(Context context) {
        List<NoteItem> items = ensureNotes(context);
        List<String> notes = new ArrayList<>();
        for (NoteItem item : items) {
            notes.add(item.text);
        }
        return notes;
    }

    void saveNotes(Context context, List<String> notes) {
        List<NoteItem> items = new ArrayList<>();
        for (String text : notes) {
            items.add(new NoteItem(UUID.randomUUID().toString(), text == null ? "" : text));
        }
        if (items.isEmpty()) {
            items.add(new NoteItem(UUID.randomUUID().toString(), ""));
        }
        saveNoteItems(context, items);
        int idx = Math.min(getSelectedIndex(context), items.size() - 1);
        setSelectedIndex(context, Math.max(0, idx));
    }

    int getSelectedIndex(Context context) {
        List<NoteItem> notes = ensureNotes(context);
        int idx = prefs(context).getInt(KEY_SELECTED_INDEX, 0);
        if (idx < 0 || idx >= notes.size()) {
            idx = 0;
            setSelectedIndex(context, idx);
        }
        return idx;
    }

    void setSelectedIndex(Context context, int index) {
        prefs(context).edit().putInt(KEY_SELECTED_INDEX, index).apply();
    }

    String getCurrentNote(Context context) {
        List<NoteItem> notes = ensureNotes(context);
        int idx = getSelectedIndex(context);
        return notes.get(idx).text;
    }

    void updateCurrentNote(Context context, String text) {
        List<NoteItem> notes = ensureNotes(context);
        int idx = getSelectedIndex(context);
        NoteItem old = notes.get(idx);
        notes.set(idx, new NoteItem(old.id, text == null ? "" : text));
        saveNoteItems(context, notes);
    }

    int addNote(Context context) {
        List<NoteItem> notes = ensureNotes(context);
        notes.add(new NoteItem(UUID.randomUUID().toString(), ""));
        int newIndex = notes.size() - 1;
        saveNoteItems(context, notes);
        setSelectedIndex(context, newIndex);
        return newIndex;
    }

    int deleteCurrentNote(Context context) {
        List<NoteItem> notes = ensureNotes(context);
        int idx = getSelectedIndex(context);
        if (notes.size() == 1) {
            NoteItem existing = notes.get(0);
            notes.set(0, new NoteItem(existing.id, ""));
            saveNoteItems(context, notes);
            return 0;
        }
        notes.remove(idx);
        int newIndex = Math.max(0, Math.min(idx, notes.size() - 1));
        saveNoteItems(context, notes);
        setSelectedIndex(context, newIndex);
        return newIndex;
    }

    int noteCount(Context context) {
        return ensureNotes(context).size();
    }

    String getSelectedNoteId(Context context) {
        List<NoteItem> notes = ensureNotes(context);
        return notes.get(getSelectedIndex(context)).id;
    }

    String getNoteIdAt(Context context, int index) {
        List<NoteItem> notes = ensureNotes(context);
        if (index < 0 || index >= notes.size()) {
            return notes.get(0).id;
        }
        return notes.get(index).id;
    }

    int getIndexByNoteId(Context context, String noteId) {
        List<NoteItem> notes = ensureNotes(context);
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id.equals(noteId)) {
                return i;
            }
        }
        return 0;
    }

    String getNoteTextById(Context context, String noteId) {
        List<NoteItem> notes = ensureNotes(context);
        for (NoteItem note : notes) {
            if (note.id.equals(noteId)) {
                return note.text;
            }
        }
        return notes.get(0).text;
    }

    // Legacy helpers kept for compatibility.
    String getStick(Context context) {
        return getCurrentNote(context);
    }

    void setStick(String textToBeSaved, Context context) {
        updateCurrentNote(context, textToBeSaved);
    }
}
