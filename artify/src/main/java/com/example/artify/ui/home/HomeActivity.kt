package com.example.artify.ui.home

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.artify.R
import com.example.artify.databinding.ActivityHomeBinding
import com.example.artify.ui.editMain.EditMainActivity
import android.content.Intent
import com.example.artify.utils.navigate
import com.example.imageaigen.ui.anime.AnimeGenActivity
import com.example.imageaigen.ui.cartoon.CartoonifyActivity
import com.example.imageaigen.ui.edit.EditImageActivity
import com.example.imageaigen.ui.generate.GenerateImageActivity
import com.example.imageaigen.ui.removebg.RemoveBackgroundActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val intent = Intent(this, EditMainActivity::class.java)
            intent.putExtra("image_uri", it.toString())
            startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Set up click listener for select photo button
        binding.btnSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.frmAnimeGen.setOnClickListener{
            this.navigate(AnimeGenActivity::class.java)
        }
        binding.frmImageGen?.setOnClickListener{
            this.navigate(GenerateImageActivity::class.java)
        }
        binding.frmEditImage?.setOnClickListener{
            this.navigate(EditImageActivity::class.java)
        }
        binding.frmBgRemove.setOnClickListener{
            this.navigate(RemoveBackgroundActivity::class.java)
        }
        binding.frmCartoonGen.setOnClickListener{
            this.navigate(CartoonifyActivity::class.java)
        }
    }
}