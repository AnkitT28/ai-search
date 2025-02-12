package com.example.aichat.view


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.aichat.R
import com.example.aichat.databinding.ActivityDashBoardBinding

class DashBoard : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityDashBoardBinding

    private var optionsCompat: ActivityOptionsCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.parseColor("#1f002b")
        window.navigationBarColor = Color.parseColor("#1f002b")

        binding.searchBar.setOnClickListener(this)
        binding.micCancelButton.setOnClickListener(this)

    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.searchBar -> {
                // Create intent to start the SearchAiClass activity
                val intent = Intent(this@DashBoard, SearchAiClass::class.java)

                // Pass the value indicating search box click
                intent.putExtra("action", "searchBox")

                // Make the scene transition animation for search bar
                optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    binding.searchBar,
                    "searchBoxAnim"
                )

                // Start the activity with the transition animation
                startActivity(intent, optionsCompat!!.toBundle())
            }

            R.id.micCancelButton -> {

                // Create intent to start the SearchAiClass activity
                val intent = Intent(this@DashBoard, SearchAiClass::class.java)

                // Pass the value indicating mic click
                intent.putExtra("action", "mic")

                // Make the scene transition animation for search bar
                optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    binding.searchBar,
                    "searchBoxAnim"
                )

                // Start the activity with the transition animation
                startActivity(intent, optionsCompat!!.toBundle())
            }
        }
    }

}