package com.example.artify.ui.profile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.artify.R
import androidx.core.graphics.drawable.toDrawable

class AvatarSelectionDialogFragment(
    private val avatarUrls: List<String>,
    private val onAvatarSelectedListener: (String) -> Unit
) : DialogFragment() {

    private lateinit var avatarAdapter: AvatarAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.avatar_selection_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    private fun setupRecyclerView() {
        avatarAdapter = AvatarAdapter(requireContext(), avatarUrls) { selectedUrl ->
            onAvatarSelectedListener(selectedUrl)
            dismiss()
        }
        recyclerView = requireView().findViewById(R.id.dialogAvatarsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 3)
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