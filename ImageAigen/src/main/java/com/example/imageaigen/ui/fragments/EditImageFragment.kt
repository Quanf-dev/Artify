package com.example.imageaigen.ui.fragments

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.databinding.FragmentEditImageBinding
import com.example.imageaigen.ui.viewmodel.GeminiViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditImageFragment : Fragment() {

    private var _binding: FragmentEditImageBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GeminiViewModel by activityViewModels()
    private var originalBitmap: Bitmap? = null
    private var editedBitmap: Bitmap? = null
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                loadImageFromUri(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditImageBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
    }
    
    fun setImageForEditing(bitmap: Bitmap) {
        originalBitmap = bitmap
        binding.originalImageView.setImageBitmap(bitmap)
        binding.editImageButton.isEnabled = true
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.editImageButton.isEnabled = !isLoading && originalBitmap != null
            
            if (activity != null) {
                // Assuming progressBar is in the activity layout
                (activity as? com.example.imageaigen.ui.GeminiImageActivity)?.toggleLoading(isLoading)
            }
        }
        
        viewModel.imageEditResult.observe(viewLifecycleOwner) { result ->
            handleEditResult(result)
        }
    }
    
    private fun setupClickListeners() {
        binding.selectImageButton.setOnClickListener {
            openImagePicker()
        }
        
        binding.editImageButton.setOnClickListener {
            originalBitmap?.let { bitmap ->
                val prompt = binding.editPromptEditText.text.toString().trim()
                if (prompt.isNotEmpty()) {
                    viewModel.editImage(bitmap, prompt)
                } else {
                    Toast.makeText(requireContext(), "Please enter an edit prompt", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.saveEditedImageButton.setOnClickListener {
            editedBitmap?.let { bitmap ->
                saveImageToGallery(bitmap)
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            binding.originalImageView.setImageBitmap(originalBitmap)
            binding.editImageButton.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleEditResult(result: GeminiResponse) {
        if (result.isError) {
            // Show error
            Toast.makeText(requireContext(), "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Handle successful response
        result.bitmap?.let { bitmap ->
            editedBitmap = bitmap
            binding.editedImageView.setImageBitmap(bitmap)
            binding.editedImageCardView.visibility = View.VISIBLE
            binding.saveEditedImageButton.visibility = View.VISIBLE
        }
    }
    
    private fun saveImageToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "GeminiEdit_${timestamp}.jpg"
        
        var outputStream: OutputStream? = null
        var saved = false
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                
                requireContext().contentResolver.also { resolver ->
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    outputStream = uri?.let { resolver.openOutputStream(it) }
                }
                
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                    saved = true
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
                
                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                    saved = true
                }
            }
            
            if (saved) {
                Toast.makeText(requireContext(), "Edited image saved to gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            outputStream?.close()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 