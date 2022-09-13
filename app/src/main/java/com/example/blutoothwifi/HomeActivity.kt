package com.example.blutoothwifi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.blutoothwifi.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sendButton.setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
        }
        binding.recieveBtn.setOnClickListener {
            startActivity(Intent(this,RecieverActivity::class.java))
        }
    }
}