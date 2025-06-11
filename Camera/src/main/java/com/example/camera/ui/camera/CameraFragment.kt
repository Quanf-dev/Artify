package com.example.camera.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.camera.R
import com.example.camera.databinding.FragmentCameraBinding
import com.example.camera.domain.model.*
import com.example.camera.ui.camera.adapter.FilterAdapter
import com.example.camera.ui.camera.adapter.FaceMaskAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber

@AndroidEntryPoint
class CameraFragment : Fragment(), EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()

    private lateinit var filterAdapter: FilterAdapter
    private lateinit var faceMaskAdapter: FaceMaskAdapter
    
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle gallery image selection
                Timber.d("Gallery image selected: $uri")
                // Navigate to edit screen or preview screen
            }
        }
    }

    private val requiredPermissions: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            else -> {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        requestCameraPermissions()
    }

    private fun setupUI() {
        setupRecyclerViews()
        setupClickListeners()
        setupSeekBars()
    }

    private fun setupRecyclerViews() {
        // Filters RecyclerView
        filterAdapter = FilterAdapter { filter ->
            viewModel.onEvent(CameraUiEvent.SelectFilter(filter))
        }
        binding.rvFilters.apply {
            adapter = filterAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        // Face Masks RecyclerView
        faceMaskAdapter = FaceMaskAdapter { mask ->
            viewModel.onEvent(CameraUiEvent.SelectMask(mask))
        }
        binding.rvFaceMasks.apply {
            adapter = faceMaskAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        // Load filters
        filterAdapter.submitList(FilterType.values().toList())
    }

    private fun setupClickListeners() {
        binding.apply {
            btnCapture.setOnClickListener {
                viewModel.onEvent(CameraUiEvent.TakePhoto)
            }

            btnSwitchCamera.setOnClickListener {
                viewModel.onEvent(CameraUiEvent.SwitchCamera)
            }

            btnFlash.setOnClickListener {
                viewModel.onEvent(CameraUiEvent.ToggleFlash)
            }

            btnTimer.setOnClickListener {
                showTimerDialog()
            }

            btnGallery.setOnClickListener {
                openGallery()
            }

            btnFaceDetection.setOnClickListener {
                viewModel.onEvent(CameraUiEvent.ToggleFaceDetection)
            }

            btnGrid.setOnClickListener {
                toggleGridLines()
            }

            tvAspectRatio.setOnClickListener {
                showAspectRatioDialog()
            }
        }
    }

    private fun setupSeekBars() {
        binding.seekBarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val zoomLevel = 1.0f + (progress / 100.0f) * 9.0f // 1x to 10x zoom
                    viewModel.onEvent(CameraUiEvent.SetZoom(zoomLevel))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Map 0-100 to -2.0 to +2.0 EV for better brightness range
                    val brightness = -2.0f + (progress / 100.0f) * 4.0f
                    viewModel.onEvent(CameraUiEvent.SetBrightness(brightness))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: CameraUiState) {
        binding.apply {
            // Loading state
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Timer countdown
            if (state.isTimerActive) {
                tvTimerCountdown.text = state.timerCountdown.toString()
                tvTimerCountdown.visibility = View.VISIBLE
            } else {
                tvTimerCountdown.visibility = View.GONE
            }

            // Flash mode icon
            val flashIcon = when (state.cameraSettings.flashMode) {
                FlashMode.OFF -> R.drawable.ic_flash_off
                FlashMode.ON -> R.drawable.ic_flash_on
                FlashMode.AUTO -> R.drawable.ic_flash_auto
            }
            btnFlash.setImageResource(flashIcon)

            // Aspect ratio text
            tvAspectRatio.text = state.cameraSettings.aspectRatio.ratio

            // Face detection button state
            btnFaceDetection.isSelected = state.isFaceDetectionEnabled

            // Face masks visibility
            rvFaceMasks.visibility = if (state.isFaceDetectionEnabled) View.VISIBLE else View.GONE
            if (state.isFaceDetectionEnabled) {
                faceMaskAdapter.submitList(state.availableMasks)
            }

            // Filter effect
            filterEffectView.setFilter(state.cameraSettings.selectedFilter)


            // Error handling
            state.errorMessage?.let { message ->
                showError(message)
                viewModel.clearError()
            }
        }
    }

    @AfterPermissionGranted(REQUEST_CODE_PERMISSIONS)
    private fun requestCameraPermissions() {
        if (EasyPermissions.hasPermissions(requireContext(), *requiredPermissions)) {
            initializeCamera()
            viewModel.onEvent(CameraUiEvent.PermissionResult(true))
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.camera_permission_required),
                REQUEST_CODE_PERMISSIONS,
                *requiredPermissions
            )
        }
    }

    private fun initializeCamera() {
        Timber.d("Initializing camera")
        // Initialize camera through repository
        viewModel.initializeCamera(binding.previewView, this)
    }

    private fun showTimerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_timer, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.timerOff).setOnClickListener {
            viewModel.onEvent(CameraUiEvent.SetTimer(0))
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.timer3s).setOnClickListener {
            viewModel.onEvent(CameraUiEvent.SetTimer(3))
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.timer5s).setOnClickListener {
            viewModel.onEvent(CameraUiEvent.SetTimer(5))
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.timer10s).setOnClickListener {
            viewModel.onEvent(CameraUiEvent.SetTimer(10))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAspectRatioDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_aspect_ratio, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.aspectRatio43).setOnClickListener {
            viewModel.onEvent(CameraUiEvent.SetAspectRatio(AspectRatio.RATIO_4_3))
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.aspectRatio169).setOnClickListener {
            viewModel.onEvent(CameraUiEvent.SetAspectRatio(AspectRatio.RATIO_16_9))
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.aspectRatio11).setOnClickListener {
            viewModel.onEvent(CameraUiEvent.SetAspectRatio(AspectRatio.RATIO_1_1))
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.aspectRatio34).setOnClickListener {
            viewModel.onEvent(CameraUiEvent.SetAspectRatio(AspectRatio.RATIO_3_4))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun toggleGridLines() {
        binding.gridLinesView.isGridVisible = !binding.gridLinesView.isGridVisible
        binding.btnGrid.isSelected = binding.gridLinesView.isGridVisible
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            initializeCamera()
            viewModel.onEvent(CameraUiEvent.PermissionResult(true))
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            viewModel.onEvent(CameraUiEvent.PermissionResult(false))
            showError(getString(R.string.camera_permissions_denied))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001

        fun newInstance(): CameraFragment {
            return CameraFragment()
        }
    }
} 