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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import android.webkit.JavascriptInterface
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1
        private const val PIXOEDITOR_REQUEST_CODE = 101 // Placeholder request code for Pixoeditor
        private const val TAG = "MainActivity"
    }

    private lateinit var imageView: ImageView
    private lateinit var applyFilterButton: Button
    private lateinit var webView: WebView
    private lateinit var mainContentLayout: ConstraintLayout
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        val selectImageButton: Button = findViewById(R.id.selectImageButton)
        applyFilterButton = findViewById(R.id.applyFilterButton)
        mainContentLayout = findViewById(R.id.mainContentLayout)
        webView = findViewById(R.id.webView)

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
                try {
                    val inputStream = contentResolver.openInputStream(currentImageUri!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap from URI: $currentImageUri")
                        Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos) // Using PNG
                    val byteArray = baos.toByteArray()
                    val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP) // NO_WRAP is important

                    // val dataUrl = "data:image/png;base64,$base64String" // Full data URL
                    // Log.d(TAG, "Converted image to Base64 Data URL (length: ${dataUrl.length})")
                    Log.d(TAG, "Converted image to Base64 string (length: ${base64String.length})")


                    mainContentLayout.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    // It's safer to pass just the Base64 string and let JS construct the data URL.
                    // Or ensure dataUrl is properly escaped if it's complex.
                    // For simplicity and safety with evaluateJavascript's string quoting:
                    webView.evaluateJavascript("javascript:loadImageForEditing('$base64String');", null)
                    // If passing full dataUrl:
                    // webView.evaluateJavascript("javascript:loadImageForEditing('$dataUrl');", null)


                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "File not found for image URI: $currentImageUri", e)
                    Toast.makeText(this, "Image file not found.", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException during image processing: $currentImageUri", e)
                    Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show()
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "OutOfMemoryError during image processing: $currentImageUri", e)
                    Toast.makeText(this, "Image is too large.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during image processing: $currentImageUri", e)
                    Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(TAG, "No image selected, cannot launch Pixoeditor.")
                Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show()
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
                            imageView.setImageURI(currentImageUri)
                            applyFilterButton.isEnabled = true // Enable button
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting image URI: ${e.message}", e)
                            Toast.makeText(this, "Error loading image.", Toast.LENGTH_SHORT).show()
                            currentImageUri = null
                            applyFilterButton.isEnabled = false // Disable button
                        }
                    } else {
                        Log.w(TAG, "Selected image URI is null")
                        Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show()
                        currentImageUri = null
                        applyFilterButton.isEnabled = false // Disable button
                    }
                } else { // Image selection cancelled or failed
                    Log.w(TAG, "Image selection cancelled or failed. ResultCode: $resultCode")
                    currentImageUri = null // Ensure currentImageUri is null
                    applyFilterButton.isEnabled = false // Disable button if no image is selected
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
            Log.d(TAG, "WebAppInterface: processEditedImage called with dataUrl (length: ${dataUrl.length})")
            try {
                if (dataUrl.startsWith("data:image")) {
                    val parts = dataUrl.split(",")
                    if (parts.size == 2) {
                        val base64String = parts[1]
                        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                        val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                        if (decodedBitmap != null) {
                            currentImageUri = null // Invalidate URI as we're using the bitmap directly
                                                // Or save bitmap to a file and get a new URI for currentImageUri

                            runOnUiThread {
                                imageView.setImageBitmap(decodedBitmap)
                                Toast.makeText(this@MainActivity, "Image updated successfully.", Toast.LENGTH_LONG).show()
                                webView.visibility = View.GONE
                                mainContentLayout.visibility = View.VISIBLE
                                applyFilterButton.isEnabled = true // Image is available for filtering again
                            }
                        } else {
                            Log.e(TAG, "WebAppInterface: Failed to decode bitmap from Base64.")
                            runOnUiThread { Toast.makeText(this@MainActivity, "Failed to process image.", Toast.LENGTH_SHORT).show() }
                        }
                    } else {
                        Log.e(TAG, "WebAppInterface: Invalid Data URL format.")
                        runOnUiThread { Toast.makeText(this@MainActivity, "Invalid image data format.", Toast.LENGTH_SHORT).show() }
                    }
                } else {
                    Log.e(TAG, "WebAppInterface: Received dataUrl does not start with 'data:image'")
                    runOnUiThread { Toast.makeText(this@MainActivity, "Invalid image data.", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "WebAppInterface: IllegalArgumentException during Base64 decode. ${e.message}", e)
                runOnUiThread { Toast.makeText(this@MainActivity, "Error decoding image.", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                Log.e(TAG, "WebAppInterface: Exception in processEditedImage. ${e.message}", e)
                runOnUiThread { Toast.makeText(this@MainActivity, "Error processing image.", Toast.LENGTH_SHORT).show() }
            } finally {
                // Ensure UI consistency even if errors occur
                if (webView.visibility == View.VISIBLE) {
                     runOnUiThread {
                        webView.visibility = View.GONE
                        mainContentLayout.visibility = View.VISIBLE
                    }
                }
            }
        }

        @JavascriptInterface
        fun editorClosed() {
            Log.d(TAG, "WebAppInterface: editorClosed called")
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Editor closed.", Toast.LENGTH_SHORT).show()
                webView.visibility = View.GONE
                mainContentLayout.visibility = View.VISIBLE
            }
        }

        @JavascriptInterface
        fun logInfo(message: String) {
            Log.i(TAG, "JS Log: $message")
        }

        @JavascriptInterface
        fun logError(message: String) {
            Log.e(TAG, "JS Error: $message")
        }

        @JavascriptInterface
        fun notifyEditorReady() {
            Log.d(TAG, "WebAppInterface: JavaScript signaled editor HTML is ready.")
            // Now it's safe to call initializeEditor in JS
            runOnUiThread {
                 webView.evaluateJavascript("javascript:initializeEditor();", null)
            }
        }
    }
}
