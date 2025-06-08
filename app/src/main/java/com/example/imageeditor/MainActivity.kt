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
            if (currentImageUri != null) {
                // applyFilters(currentImageUri!!) // Uncomment when implemented
                Toast.makeText(this, "Filter functionality not yet implemented.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Apply filter button clicked for image: $currentImageUri")
            } else {
                Toast.makeText(this, "No image selected to apply filter.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // private fun applyFilters(imageUri: Uri) {
    //     // TODO: Implement filter logic
    //     Log.d(TAG, "applyFilters called with URI: $imageUri")
    // }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                currentImageUri = selectedImageUri
                Log.d(TAG, "Selected image URI: $currentImageUri")
                try {
                    imageView.setImageURI(currentImageUri)
                    applyFilterButton.isEnabled = true
                    // Or applyFilterButton.visibility = View.VISIBLE if it was GONE
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
}
