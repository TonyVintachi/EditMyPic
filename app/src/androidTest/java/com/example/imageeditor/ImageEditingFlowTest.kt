package com.example.imageeditor

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
// import androidx.test.espresso.intent.rule.IntentsTestRule // Commented out as per preference for ActivityScenarioRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider // Added for helper
import android.content.Context // Added for helper


@RunWith(AndroidJUnit4::class)
class ImageEditingFlowTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testImageSelection_displaysPreviewAndLoadButton() {
        // 1. Verify initial UI state
        onView(withId(R.id.selectImageButton)).check(matches(isDisplayed()))
        onView(withId(R.id.imageCardView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.applyFilterButton)).check(matches(not(isDisplayed())))
        onView(withId(R.id.cancelEditingButton)).check(matches(not(isDisplayed()))) // Also check cancel button
        onView(withId(R.id.webView)).check(matches(not(isDisplayed())))

        // 2. Prepare a result for the image picker intent
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = targetContext.packageName
        val imageUri = Uri.parse("android.resource://" + packageName + "/" + R.drawable.ic_launcher_background)

        val resultData = Intent()
        resultData.data = imageUri
        val activityResult = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)

        // 3. Stub out the intent that picks images
        Intents.intending(hasAction(Intent.ACTION_PICK)).respondWith(activityResult)

        // 4. Click the "Select Image" button
        onView(withId(R.id.selectImageButton)).perform(click())

        // 5. Verify UI changes after image selection
        onView(withId(R.id.imageCardView)).check(matches(isDisplayed()))
        onView(withId(R.id.applyFilterButton)).check(matches(isDisplayed()))
        onView(withId(R.id.applyFilterButton)).check(matches(isEnabled()))
        onView(withId(R.id.applyFilterButton)).check(matches(withText("Load Image into Editor")))
        onView(withId(R.id.cancelEditingButton)).check(matches(not(isDisplayed()))) // Cancel button should still be hidden
        onView(withId(R.id.webView)).check(matches(not(isDisplayed())))

        onView(withId(R.id.imageView)).check(matches(ImageViewHasDrawableMatcher.hasDrawable()))
    }

    // Helper method to perform image selection
    private fun selectImage() {
        // Prepare a result for the image picker intent
        // Using ApplicationProvider.getApplicationContext() to ensure correct context for package name
        val packageName = ApplicationProvider.getApplicationContext<Context>().packageName
        val imageUri = Uri.parse("android.resource://" + packageName + "/" + R.drawable.ic_launcher_background)
        val resultData = Intent()
        resultData.data = imageUri
        val activityResult = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)

        // Stub out the intent
        Intents.intending(hasAction(Intent.ACTION_PICK)).respondWith(activityResult)

        // Click the "Select Image" button
        onView(withId(R.id.selectImageButton)).perform(click())

        // Basic verification that selection worked
        onView(withId(R.id.imageCardView)).check(matches(isDisplayed()))
        onView(withId(R.id.applyFilterButton)).check(matches(isDisplayed()))
    }

    @Test
    fun testLoadImageIntoEditor_showsWebViewAndHidesPreview() {
        // 1. Perform image selection first
        selectImage()

        // 2. Verify initial state before loading into editor (after image selection)
        onView(withId(R.id.applyFilterButton)).check(matches(isDisplayed()))
        onView(withId(R.id.applyFilterButton)).check(matches(isEnabled()))
        onView(withId(R.id.webView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.cancelEditingButton)).check(matches(not(isDisplayed())))

        // 3. Click the "Load Image into Editor" button
        onView(withId(R.id.applyFilterButton)).perform(click())

        // 4. Verify UI changes after loading into editor
        //    ProgressBar might appear briefly. Espresso usually handles waiting for UI thread to settle.
        //    IdlingResource might be needed for webView content if JS operations are long.

        // Wait for potential ProgressBar to disappear and JS to signal readiness (which hides progress bar)
        // This is a simple check, might need IdlingResource for real JS loading.
        // For now, we assume that if webView is visible, the main processing for visibility is done.
        onView(withId(R.id.webView)).check(matches(isDisplayed()))
        onView(withId(R.id.imageCardView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.applyFilterButton)).check(matches(not(isDisplayed())))
        onView(withId(R.id.cancelEditingButton)).check(matches(isDisplayed()))
    }

    @Test
    fun testCancelEditing_hidesWebViewAndResetsUI() {
        // 1. Perform image selection
        selectImage()

        // 2. Click "Load Image into Editor" to show the WebView
        onView(withId(R.id.applyFilterButton)).perform(click())

        // 3. Verify editor state (WebView and Cancel button are visible)
        onView(withId(R.id.webView)).check(matches(isDisplayed()))
        onView(withId(R.id.cancelEditingButton)).check(matches(isDisplayed()))
        onView(withId(R.id.imageCardView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.applyFilterButton)).check(matches(not(isDisplayed())))


        // 4. Click the "Cancel Edits" button
        //    We assume Pixo's JS calls back to Android (editorCancelled or editorClosed)
        //    which then updates the UI. Espresso waits for UI thread idle.
        onView(withId(R.id.cancelEditingButton)).perform(click())

        // 5. Verify UI changes after cancelling
        onView(withId(R.id.webView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.cancelEditingButton)).check(matches(not(isDisplayed())))

        onView(withId(R.id.imageCardView)).check(matches(isDisplayed())) // Preview should reappear
        onView(withId(R.id.applyFilterButton)).check(matches(isDisplayed())) // "Load Image" button should reappear
        onView(withId(R.id.applyFilterButton)).check(matches(isEnabled()))

        // Verify the image in imageCardView is still the original one (or last saved if that was the flow)
        onView(withId(R.id.imageView)).check(matches(ImageViewHasDrawableMatcher.hasDrawable()))
    }
}
