package com.zfs.slidetoggleview.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.zfs.slidetoggleview.R
import com.zfs.slidetoggleview.SlideToggleView
import com.zfs.slidetoggleview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SlideToggleView.SlideToggleListener,
    SlideToggleView.SlideReleasedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toggle.apply {
            addLifecycleScope(this@MainActivity.lifecycleScope)
            setSlideToggleListener(this@MainActivity)
            setSlideReleaseListener(this@MainActivity)
        }
    }

    override fun onSlideListener(view: SlideToggleView?, leftOrRight: Int) {
        // TODO: 2021/10/22
    }

    override suspend fun onOpenBeforeListener(): Boolean {
        return false
    }

    override suspend fun onCloseBeforeListener(): Boolean {
        return false
    }
}