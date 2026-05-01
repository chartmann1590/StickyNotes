package com.charles.stickynotes;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void saveNote_persistsContent() throws Exception {
        String unique = "ci-note-" + System.currentTimeMillis();
        onView(withId(R.id.note_editor)).perform(typeText(unique), closeSoftKeyboard());
        onView(withId(R.id.floating_save_button)).perform(click());

        Context ctx = ApplicationProvider.getApplicationContext();
        StickyNote repo = new StickyNote();
        String saved = repo.getCurrentNote(ctx);
        assertTrue(saved.contains(unique));
    }

    @Test
    public void useAppContext() {
        Context appContext = ApplicationProvider.getApplicationContext();
        assertTrue(appContext.getPackageName().contains("com.charles.stickynotes"));
    }
}
