package com.example.imageeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import android.webkit.JavascriptInterface
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import java.io.OutputStream
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager


class MainActivity : AppCompatActivity() {

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1
        private const val PIXOEDITOR_REQUEST_CODE = 101 // Placeholder request code for Pixoeditor
        private const val WRITE_STORAGE_PERMISSION_REQUEST_CODE = 102
        private const val TAG = "MainActivity"
    }

    private lateinit var imageView: ImageView
    private lateinit var applyFilterButton: com.google.android.material.button.MaterialButton
    private lateinit var cancelEditingButton: com.google.android.material.button.MaterialButton // Added
    private lateinit var webView: WebView
    // private lateinit var mainContentLayout: ConstraintLayout // Removed
    private lateinit var progressBar: ProgressBar
    // private lateinit var dimOverlay: View // Removed
    private lateinit var imageCardView: com.google.android.material.card.MaterialCardView // Added

    private var currentImageUri: Uri? = null
    private var pixoJsReady: Boolean = false // Added flag

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        imageCardView = findViewById(R.id.imageCardView) // Added
        val selectImageButton: Button = findViewById(R.id.selectImageButton)
        applyFilterButton = findViewById(R.id.applyFilterButton)
        cancelEditingButton = findViewById(R.id.cancelEditingButton) // Added
        // mainContentLayout = findViewById(R.id.mainContentLayout) // Removed
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        // dimOverlay = findViewById(R.id.dimOverlay) // Removed

        // Initial UI State
        imageCardView.visibility = View.GONE
        applyFilterButton.visibility = View.GONE
        cancelEditingButton.visibility = View.GONE // Added
        webView.visibility = View.GONE

        // Configure WebView settings
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true // Important for Pixo or other web apps that use local storage
        webView.settings.allowFileAccess = true // May be needed for some WebView functionalities
        webView.settings.allowContentAccess = true
        webView.webViewClient = WebViewClient() // Basic client to open links in this WebView
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Load the local HTML file - initial load, JS will call back when ready for Pixo init
        webView.loadUrl("file:///android_asset/editor.html")


        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }

        applyFilterButton.setOnClickListener {
            if (currentImageUri != null) {
                progressBar.visibility = View.VISIBLE
                applyFilterButton.isEnabled = false // Prevent multiple clicks
                // cancelEditingButton.isEnabled = false; // Optional: disable while this primary action processes

                try {
                    val inputStream = contentResolver.openInputStream(currentImageUri!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap from URI: $currentImageUri")
                        Snackbar.make(findViewById(android.R.id.content), "Error: Failed to load image.", Snackbar.LENGTH_LONG).show()
                        progressBar.visibility = View.GONE
                        applyFilterButton.isEnabled = true
                        return@setOnClickListener
                    }

                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos) // Using PNG
                    val byteArray = baos.toByteArray()
                    val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP) // NO_WRAP is important

                    // val dataUrl = "data:image/png;base64,$base64String" // Full data URL
                    // Log.d(TAG, "Converted image to Base64 Data URL (length: ${dataUrl.length})")
                    Log.d(TAG, "Converted image to Base64 string (length: ${base64String.length})")

                    webView.visibility = View.VISIBLE
                    cancelEditingButton.visibility = View.VISIBLE // Show cancel button
                    webView.evaluateJavascript("javascript:loadImageForEditing('$base64String');", null)

                    imageCardView.visibility = View.GONE
                    applyFilterButton.visibility = View.GONE


                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "File not found for image URI: $currentImageUri", e)
                    Snackbar.make(findViewById(android.R.id.content), "Error: Image file not found.", Snackbar.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    applyFilterButton.isEnabled = true
                    // cancelEditingButton.visibility = View.GONE; // Or keep visible if needed
                    webView.visibility = View.GONE
                    imageCardView.visibility = View.VISIBLE
                    applyFilterButton.visibility = View.VISIBLE
                } catch (e: IOException) {
                    Log.e(TAG, "IOException during image processing: $currentImageUri", e)
                    Snackbar.make(findViewById(android.R.id.content), "Error: Could not read image file.", Snackbar.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    applyFilterButton.isEnabled = true
                    // cancelEditingButton.visibility = View.GONE;
                    webView.visibility = View.GONE
                    imageCardView.visibility = View.VISIBLE
                    applyFilterButton.visibility = View.VISIBLE
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "OutOfMemoryError during image processing: $currentImageUri", e)
                    Snackbar.make(findViewById(android.R.id.content), "Error: Image too large to process.", Snackbar.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    applyFilterButton.isEnabled = true
                    // cancelEditingButton.visibility = View.GONE;
                    webView.visibility = View.GONE
                    imageCardView.visibility = View.VISIBLE
                    applyFilterButton.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during image processing: $currentImageUri", e)
                    Snackbar.make(findViewById(android.R.id.content), "Error: Could not prepare image for editor.", Snackbar.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    applyFilterButton.isEnabled = true
                    // cancelEditingButton.visibility = View.GONE; still adjusting view
                    webView.visibility = View.GONE
                    imageCardView.visibility = View.VISIBLE
                    applyFilterButton.visibility = View.VISIBLE
                }
            } else {
                Log.w(TAG, "No image selected.")
                applyFilterButton.isEnabled = true;
                Snackbar.make(findViewById(android.R.id.content), "Please select an image first.", Snackbar.LENGTH_SHORT).show()
            }
        }

        cancelEditingButton.setOnClickListener {
            if (webView.visibility == View.VISIBLE && pixoJsReady) {
                Log.d(TAG, "Cancel Edits button clicked. Calling JS:triggerPixoCancel()")
                webView.evaluateJavascript("javascript:triggerPixoCancel();", null)
                // UI changes like hiding webview will be handled by editorClosed callback
            } else {
                Log.d(TAG, "Cancel Edits button clicked, but editor not ready or not visible.")
                // Fallback: hide webview and show main content if something is out of sync
                webView.visibility = View.GONE
                cancelEditingButton.visibility = View.GONE
                imageCardView.visibility = View.VISIBLE
                applyFilterButton.visibility = View.VISIBLE
                applyFilterButton.isEnabled = true
                pixoJsReady = false
            }
        }

        initializePixoeditorSDK() // This is a placeholder for native SDK init, not WebView related for now
    }

    private fun initializePixoeditorSDK() {
        // This is a placeholder for Pixoeditor SDK initialization.
        // It requires the actual Pixoeditor SDK to be integrated into the project.
        // Pixoeditor.initialize(this, "29q52bi3p728") // Replace with actual SDK initialization call and API key
        Log.d(TAG, "Attempting to initialize Pixoeditor SDK (placeholder)")
    }

    // private fun applyFilters(imageUri: Uri) {
    //     // TODO: Implement filter logic
    //     Log.d(TAG, "applyFilters called with URI: $imageUri")
    // }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_IMAGE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val selectedImageUri: Uri? = data?.data
                    if (selectedImageUri != null) {
                        currentImageUri = selectedImageUri
                        Log.d(TAG, "Selected image URI: $currentImageUri")
                        try {
                            imageCardView.visibility = View.VISIBLE
                            imageView.setImageURI(currentImageUri)
                            applyFilterButton.text = "Load Image into Editor"
                            applyFilterButton.visibility = View.VISIBLE
                            applyFilterButton.isEnabled = true
                            webView.visibility = View.GONE
                            cancelEditingButton.visibility = View.GONE // Hide cancel button
                            pixoJsReady = false
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting image URI: ${e.message}", e)
                            Snackbar.make(findViewById(android.R.id.content), "Error loading image.", Snackbar.LENGTH_SHORT).show()
                            currentImageUri = null
                            imageCardView.visibility = View.GONE
                            applyFilterButton.visibility = View.GONE
                            applyFilterButton.isEnabled = false
                            cancelEditingButton.visibility = View.GONE
                            webView.visibility = View.GONE
                            progressBar.visibility = View.GONE
                            pixoJsReady = false
                        }
                    } else {
                        Log.w(TAG, "Selected image URI is null")
                        Snackbar.make(findViewById(android.R.id.content), "No image selected.", Snackbar.LENGTH_SHORT).show()
                        currentImageUri = null
                        imageCardView.visibility = View.GONE
                        applyFilterButton.visibility = View.GONE
                        applyFilterButton.isEnabled = false
                        cancelEditingButton.visibility = View.GONE
                        webView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        pixoJsReady = false
                    }
                } else { // Image selection cancelled or failed
                    Log.w(TAG, "Image selection cancelled or failed. ResultCode: $resultCode")
                    currentImageUri = null
                    imageCardView.visibility = View.GONE
                    applyFilterButton.visibility = View.GONE
                    applyFilterButton.isEnabled = false
                    cancelEditingButton.visibility = View.GONE
                    webView.visibility = View.GONE
                    progressBar.visibility = View.GONE
                    pixoJsReady = false
                }
            }
            PIXOEDITOR_REQUEST_CODE -> { // This is for the native SDK placeholder, not WebView
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // val editedImageUri = data.getParcelableExtra<Uri>(Pixoeditor.EDITED_IMAGE_URI_RESULT) // Replace with actual result key
                    val editedImageUri: Uri? = data.data // Placeholder: using data.data for now, replace with actual key
                    if (editedImageUri != null) {
                        Log.d(TAG, "Pixoeditor returned result. Attempting to load edited image (placeholder). Uri: $editedImageUri")
                        currentImageUri = editedImageUri // Update current URI
                        try {
                            imageView.setImageURI(currentImageUri)
                            // TODO: Save the editedImageUri to storage if needed
                            Log.d(TAG, "Successfully set edited image to ImageView.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting edited image URI: ${e.message}", e)
                            Toast.makeText(this, "Error displaying edited image.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w(TAG, "Pixoeditor returned OK but editedImageUri is null.")
                        Toast.makeText(this, "Failed to retrieve edited image.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Pixoeditor editing was cancelled or failed. ResultCode: $resultCode")
                    // Optionally, show a toast to the user
                    // Toast.makeText(this, "Image editing cancelled.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- WebAppInterface ---
    inner class WebAppInterface {
        @JavascriptInterface
        fun processEditedImage(dataUrl: String) {
            runOnUiThread { progressBar.visibility = View.VISIBLE }
            Log.d(TAG, "WebAppInterface: processEditedImage called with dataUrl (length: ${dataUrl.length})")
            try {
                val mimeType = DataUrlUtils.parseMimeType(dataUrl)
                if (mimeType != null) {
                    Log.d(TAG, "Received image with MIME type: $mimeType from Pixoeditor")
                } else {
                    Log.w(TAG, "Could not parse MIME type from Data URL.")
                    // Consider this an error if a MIME type is strictly expected
                    // For now, we'll proceed, but saving might be affected if MIME type is crucial later
                }

                if (dataUrl.startsWith("data:image")) { // Check it's an image (even if mimeType parsing failed, could be malformed but still an image)
                    val base64String = dataUrl.substringAfter("base64,")
                    if (base64String.isNotEmpty()) {
                        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                        val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                        if (decodedBitmap != null) {
                            currentImageUri = null // Invalidate URI as we're using the bitmap directly
                                                // Or save bitmap to a file and get a new URI for currentImageUri

                            runOnUiThread {
                                imageCardView.visibility = View.VISIBLE
                                imageView.setImageBitmap(decodedBitmap)
                                saveBitmapToGallery(decodedBitmap)
                                Snackbar.make(findViewById(android.R.id.content), "Image updated successfully.", Snackbar.LENGTH_LONG).show()
                                webView.visibility = View.GONE
                                cancelEditingButton.visibility = View.GONE
                                applyFilterButton.text = "Load Image into Editor"
                                applyFilterButton.visibility = View.VISIBLE
                                applyFilterButton.isEnabled = true
                                pixoJsReady = false
                            }
                        } else {
                            Log.e(TAG, "WebAppInterface: Failed to decode bitmap from Base64.")
                            // Keep editor visible for user to retry or close
                            runOnUiThread {
                                webView.visibility = View.VISIBLE // Keep editor open
                                imageCardView.visibility = View.GONE
                                applyFilterButton.visibility = View.GONE
                                cancelEditingButton.visibility = View.VISIBLE // Allow cancel
                                Snackbar.make(findViewById(android.R.id.content), "Error: Failed to process image.", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Log.e(TAG, "WebAppInterface: Invalid Data URL format.")
                         runOnUiThread {
                            webView.visibility = View.VISIBLE
                            imageCardView.visibility = View.GONE
                            applyFilterButton.visibility = View.GONE
                            cancelEditingButton.visibility = View.VISIBLE
                            Snackbar.make(findViewById(android.R.id.content), "Error: Invalid image data format.", Snackbar.LENGTH_LONG).show()
                         }
                    }
                } else {
                    Log.e(TAG, "WebAppInterface: Received dataUrl does not start with 'data:image'")
                    runOnUiThread {
                        webView.visibility = View.VISIBLE
                        imageCardView.visibility = View.GONE
                        applyFilterButton.visibility = View.GONE
                        cancelEditingButton.visibility = View.VISIBLE
                        Snackbar.make(findViewById(android.R.id.content), "Error: Invalid image data.", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "WebAppInterface: IllegalArgumentException during Base64 decode. ${e.message}", e)
                runOnUiThread {
                    webView.visibility = View.VISIBLE
                    imageCardView.visibility = View.GONE
                    applyFilterButton.visibility = View.GONE
                    cancelEditingButton.visibility = View.VISIBLE
                    Snackbar.make(findViewById(android.R.id.content), "Error: Failed to process edited image data.", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebAppInterface: Exception in processEditedImage. ${e.message}", e)
                runOnUiThread {
                    webView.visibility = View.VISIBLE
                    imageCardView.visibility = View.GONE
                    applyFilterButton.visibility = View.GONE
                    cancelEditingButton.visibility = View.VISIBLE
                    Snackbar.make(findViewById(android.R.id.content), "Error: Could not display edited image.", Snackbar.LENGTH_LONG).show()
                }
            } finally {
                if (progressBar.visibility == View.VISIBLE) {
                     runOnUiThread { progressBar.visibility = View.GONE }
                }
            }
        }

        @JavascriptInterface
        fun editorClosed() {
            Log.d(TAG, "WebAppInterface: editorClosed called")
            runOnUiThread {
                Snackbar.make(findViewById(android.R.id.content), "Editor session ended.", Snackbar.LENGTH_SHORT).show()
                webView.visibility = View.GONE
                cancelEditingButton.visibility = View.GONE
                imageCardView.visibility = View.VISIBLE
                applyFilterButton.text = "Load Image into Editor"
                applyFilterButton.visibility = View.VISIBLE
                applyFilterButton.isEnabled = true
                progressBar.visibility = View.GONE
                pixoJsReady = false
            }
        }

        @JavascriptInterface
        fun editorCancelled() {
            Log.d(TAG, "WebAppInterface: editorCancelled called from Pixoeditor.")
            runOnUiThread {
                Snackbar.make(findViewById(android.R.id.content), "Editing cancelled.", Snackbar.LENGTH_SHORT).show()
                webView.visibility = View.GONE
                imageCardView.visibility = View.VISIBLE // Show the last known state of the image (unedited or last saved)
                applyFilterButton.visibility = View.VISIBLE
                applyFilterButton.isEnabled = true
                cancelEditingButton.visibility = View.GONE
                progressBar.visibility = View.GONE // Ensure progress bar is hidden
                pixoJsReady = false // Editor session is over
            }
        }

        @JavascriptInterface
        fun logInfo(message: String) {
            Log.i(TAG, "JS Log: $message")
        }

        @JavascriptInterface
        fun logError(message: String) {
            Log.e(TAG, "JS Error: $message")
            runOnUiThread { Snackbar.make(findViewById(android.R.id.content), "Editor error: $message", Snackbar.LENGTH_LONG).show() }
        }

        @JavascriptInterface
        fun notifyEditorReady() {
            Log.d(TAG, "WebAppInterface: JavaScript signaled editor HTML is ready.")
            // Now it's safe to call initializeEditor in JS, then hide progress bar
            runOnUiThread {
                 webView.evaluateJavascript("javascript:initializeEditor();", null)
                 // It might be too soon to hide here if initializeEditor itself is async
                 // but for now, this is when we know JS is ready to proceed.
                 // A better approach: JS calls back another function after its own init is complete.
                 progressBar.visibility = View.GONE
                 applyFilterButton.isEnabled = true // Re-enable button as editor is ready
            }
        }
    }

    private fun checkAndRequestStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // MediaStore is preferred for Q+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_STORAGE_PERMISSION_REQUEST_CODE)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), "Storage permission granted. You can try saving again if it failed (e.g. by re-processing).", Snackbar.LENGTH_LONG).show()
                // Potentially trigger save again if bitmap is stored, or inform user.
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Storage permission denied. Image cannot be saved to gallery.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || checkAndRequestStoragePermission()) {
            val filename = "PixoEdited_${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null
            var imageUri: Uri? = null
            val resolver = contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ImageEditorApp") // Changed MyAppName
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            try {
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
                fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    imageUri?.let { resolver.update(it, contentValues, null, null) }
                }
                Snackbar.make(findViewById(android.R.id.content), "Image saved to Gallery", Snackbar.LENGTH_SHORT).show()
                Log.d(TAG, "Image saved to gallery: $imageUri")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image to gallery", e)
                Snackbar.make(findViewById(android.R.id.content), "Error saving image: ${e.message}", Snackbar.LENGTH_LONG).show()
                // Clean up pending entry if error occurs on Q+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && imageUri != null) {
                     resolver.delete(imageUri, null, null)
                }
            } finally {
                try {
                    fos?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing FileOutputStream", e)
                }
            }
        } else {
             Snackbar.make(findViewById(android.R.id.content), "Storage permission needed to save image.", Snackbar.LENGTH_LONG).show()
        }
    }
}
