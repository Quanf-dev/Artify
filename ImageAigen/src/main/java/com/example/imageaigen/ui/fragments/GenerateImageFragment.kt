package com.example.imageaigen.ui.fragments

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.imageaigen.data.model.GeminiResponse
import com.example.imageaigen.databinding.FragmentGenerateImageBinding
import com.example.imageaigen.ui.viewmodel.GeminiViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerateImageFragment : Fragment() {

    private var _binding: FragmentGenerateImageBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GeminiViewModel by activityViewModels()
    private var generatedBitmap: Bitmap? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateImageBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.generateButton.isEnabled = !isLoading
            
            if (activity != null) {
                // Assuming progressBar is in the activity layout
                (activity as? com.example.imageaigen.ui.GeminiImageActivity)?.toggleLoading(isLoading)
            }
        }
        
        viewModel.imageGenerationResult.observe(viewLifecycleOwner) { result ->
            handleGenerationResult(result)
        }
    }
    
    private fun setupClickListeners() {
        binding.generateButton.setOnClickListener {
            val prompt = binding.promptEditText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                viewModel.generateImage(prompt)
            } else {
                Toast.makeText(requireContext(), "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.saveImageButton.setOnClickListener {
            generatedBitmap?.let { bitmap ->
                saveImageToGallery(bitmap)
            }
        }
        
        binding.editImageButton.setOnClickListener {
            generatedBitmap?.let { bitmap ->
                // Navigate to edit tab with this image
                (activity as? com.example.imageaigen.ui.GeminiImageActivity)?.navigateToEditWithImage(bitmap)
            }
        }
    }
    
    private fun handleGenerationResult(result: GeminiResponse) {
        if (result.isError) {
            // Show error
            Toast.makeText(requireContext(), "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Handle successful response
        result.bitmap?.let { bitmap ->
            generatedBitmap = bitmap
            binding.generatedImageView.setImageBitmap(bitmap)
            binding.generatedImageView.visibility = View.VISIBLE
            binding.actionButtonsLayout.visibility = View.VISIBLE
        }
        
        result.text?.let { text ->
            if (text.isNotEmpty()) {
                binding.responseTextView.text = text
                binding.responseCardView.visibility = View.VISIBLE
            }
        }
    }
    
    private fun saveImageToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "Gemini_${timestamp}.jpg"
        
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
                Toast.makeText(requireContext(), "Image saved to gallery", Toast.LENGTH_SHORT).show()
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