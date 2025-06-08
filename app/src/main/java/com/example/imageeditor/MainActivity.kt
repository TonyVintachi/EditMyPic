package com.example.imageeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1
        private const val PIXOEDITOR_REQUEST_CODE = 101 // Placeholder request code for Pixoeditor
        private const val TAG = "MainActivity"
    }

    private lateinit var imageView: ImageView
    private lateinit var applyFilterButton: Button
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        val selectImageButton: Button = findViewById(R.id.selectImageButton)
        applyFilterButton = findViewById(R.id.applyFilterButton)

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }

        applyFilterButton.setOnClickListener {
            // if (currentImageUri != null) {
            //     // val intent = Pixoeditor.IntentBuilder(this, currentImageUri) // Or however the SDK launches
            //     //         .setApiKey("29q52bi3p728") // If API key is needed at launch
            //     //         .build()
            //     // startActivityForResult(intent, PIXOEDITOR_REQUEST_CODE) // Define PIXOEDITOR_REQUEST_CODE
            //     Log.d(TAG, "Attempting to launch Pixoeditor with URI: $currentImageUri (placeholder)")
            // } else {
            //     Log.w(TAG, "No image selected, cannot launch Pixoeditor.")
            //     Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show()
            // }
            if (currentImageUri != null) {
                Log.d(TAG, "Attempting to launch Pixoeditor with URI: $currentImageUri (placeholder)")
                Toast.makeText(this, "Pixoeditor launch (placeholder) for $currentImageUri", Toast.LENGTH_LONG).show() // Kept a toast for user feedback for now
            } else {
                Log.w(TAG, "No image selected, cannot launch Pixoeditor.")
                Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show()
            }
        }

        initializePixoeditorSDK()
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
                            applyFilterButton.isEnabled = true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting image URI: ${e.message}", e)
                            Toast.makeText(this, "Error loading image.", Toast.LENGTH_SHORT).show()
                            currentImageUri = null
                            applyFilterButton.isEnabled = false
                        }
                    } else {
                        Log.w(TAG, "Selected image URI is null")
                        Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show()
                        currentImageUri = null
                        applyFilterButton.isEnabled = false
                    }
                }
            }
            PIXOEDITOR_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // val editedImageUri = data.getParcelableExtra<Uri>(Pixoeditor.EDITED_IMAGE_URI_RESULT) // Replace with actual result key
                    val editedImageUri: Uri? = data.data // Placeholder: using data.data for now, replace with actual key
                    if (editedImageUri != null) {
                        Log.d(TAG, "Pixoeditor returned result. Attempting to load edited image (placeholder). Uri: $editedImageUri")
                        currentImageUri = editedImageUri
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
}
