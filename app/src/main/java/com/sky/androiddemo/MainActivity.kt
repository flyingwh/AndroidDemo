package com.sky.androiddemo

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sky.androiddemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val effects = listOf("No Effect", "Blur", "ColorMatrix", "Offset", "Chain")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, effects)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.effectSpinner.adapter = adapter

        binding.effectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateEffect(effects[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
        // Set initial effect
        updateEffect(effects[0])
    }

    private fun updateEffect(effectName: String) {
        binding.seekbarContainer.removeAllViews()
        binding.imageView.setRenderEffect(null)
        when (effectName) {
            "No Effect" -> {
                // No effect, do nothing
            }
            "Blur" -> setupBlurEffect()
            "ColorMatrix" -> setupColorMatrixEffect()
            "Offset" -> setupOffsetEffect()
            "Chain" -> setupChainEffect()
        }
    }

    private fun setupBlurEffect() {
        val params = mutableMapOf<String, Float>()
        params["Radius"] = 10f
        addSeekBar("Radius", 0f, 100f, 10f) { value ->
            params["Radius"] = value
            val radius = params["Radius"]!!
            if (radius > 0) {
                binding.imageView.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR))
            } else {
                binding.imageView.setRenderEffect(null)
            }
        }
    }

    private fun setupColorMatrixEffect() {
        val params = mutableMapOf("R" to 1f, "G" to 1f, "B" to 1f, "A" to 1f)
        val labels = arrayOf("R", "G", "B", "A")
        labels.forEach { label ->
            addSeekBar(label, 0f, 2f, 1f) { value ->
                params[label] = value
                val matrix = ColorMatrix()
                matrix.setScale(params["R"]!!, params["G"]!!, params["B"]!!, params["A"]!!)
                binding.imageView.setRenderEffect(RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix)))
            }
        }
    }

    private fun setupOffsetEffect() {
        val params = mutableMapOf("X" to 0f, "Y" to 0f)
        addSeekBar("X", -100f, 100f, 0f) { value ->
            params["X"] = value
            binding.imageView.setRenderEffect(RenderEffect.createOffsetEffect(params["X"]!!, params["Y"]!!))
        }
        addSeekBar("Y", -100f, 100f, 0f) { value ->
            params["Y"] = value
            binding.imageView.setRenderEffect(RenderEffect.createOffsetEffect(params["X"]!!, params["Y"]!!))
        }
    }

    private fun setupChainEffect() {
        val blurParams = mutableMapOf("Radius" to 10f)
        val colorParams = mutableMapOf("R" to 1f, "G" to 1f, "B" to 1f, "A" to 1f)

        addSeekBar("Blur Radius", 0f, 100f, 10f) { value ->
            blurParams["Radius"] = value
            updateChainEffect(blurParams, colorParams)
        }

        val labels = arrayOf("R", "G", "B", "A")
        labels.forEach { label ->
            addSeekBar(label, 0f, 2f, 1f) { value ->
                colorParams[label] = value
                updateChainEffect(blurParams, colorParams)
            }
        }
    }

    private fun updateChainEffect(blurParams: Map<String, Float>, colorParams: Map<String, Float>) {
        val blurRadius = blurParams["Radius"]!!
        val blurEffect = if (blurRadius > 0) {
            RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR)
        } else {
            null
        }

        val matrix = ColorMatrix()
        matrix.setScale(colorParams["R"]!!, colorParams["G"]!!, colorParams["B"]!!, colorParams["A"]!!)
        val colorEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))

        val chainEffect = if (blurEffect != null) {
            RenderEffect.createChainEffect(colorEffect, blurEffect)
        } else {
            colorEffect
        }
        binding.imageView.setRenderEffect(chainEffect)
    }

    private fun addSeekBar(label: String, min: Float, max: Float, initial: Float, onUpdate: (Float) -> Unit) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.setPadding(0, 16, 0, 16)

        val textView = TextView(this)
        textView.text = label
        textView.width = 150
        layout.addView(textView)

        val seekBar = SeekBar(this)
        val range = (max - min) * 10
        seekBar.max = range.toInt()
        seekBar.progress = ((initial - min) * 10).toInt()

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.weight = 1f
        seekBar.layoutParams = params
        layout.addView(seekBar)

        binding.seekbarContainer.addView(layout)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = min + progress / 10f
                onUpdate(value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Set initial value
        onUpdate(initial)
    }
}
