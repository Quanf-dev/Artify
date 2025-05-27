package com.example.artify.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.artify.R

class AvatarSelectionDialogFragment(
    private val avatarUrls: List<String>,
    private val onAvatarSelectedListener: (String) -> Unit
) : DialogFragment() {

    private lateinit var avatarAdapter: AvatarAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.avatar_selection_dialog, container, false)
        recyclerView = view.findViewById(R.id.dialogAvatarsRecyclerView)
        
        // Add cancel button handler
//        view.findViewById<Button>(R.id.btnCancel)?.setOnClickListener {
//            dismiss()
//        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // Prevent dialog from dismissing when clicking outside
        dialog?.setCanceledOnTouchOutside(false)
    }

    private fun setupRecyclerView() {
        avatarAdapter = AvatarAdapter(requireContext(), avatarUrls) { selectedUrl ->
            onAvatarSelectedListener(selectedUrl)
            dismiss() // Only dismiss when an avatar is selected
        }
        recyclerView.layoutManager = GridLayoutManager(context, 3) // Changed from 4 to 3 columns
        recyclerView.adapter = avatarAdapter
    }

    companion object {
        const val TAG = "AvatarSelectionDialog"

        fun newInstance(
            avatarUrls: List<String>,
            listener: (String) -> Unit
        ): AvatarSelectionDialogFragment {
            return AvatarSelectionDialogFragment(avatarUrls, listener)
        }
    }
} 