package com.drowsiness.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.drowsiness.app.databinding.FragmentDashboardBinding
import com.drowsiness.app.detector.FaceAnalyzer
import com.drowsiness.app.viewmodel.DetectionState
import com.drowsiness.app.viewmodel.MainViewModel
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var cameraExecutor: ExecutorService
    private var frameIndex = 0
    private var cameraStarted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupCharts()
        setupToggleButton()
        observeState()

        // Only start camera if permission is already granted
        if (hasCameraPermission()) {
            setupCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        // Start camera if permission was just granted (after returning from permission dialog)
        if (!cameraStarted && hasCameraPermission()) {
            setupCamera()
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    /** Called by MainActivity after the user grants camera permission */
    fun onCameraPermissionGranted() {
        if (!cameraStarted && isAdded) {
            setupCamera()
        }
    }

    private fun setupToggleButton() {
        binding.btnToggle.setOnClickListener {
            viewModel.toggle()
        }
    }

    private fun setupCamera() {
        cameraStarted = true
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor, FaceAnalyzer { ear, status ->
                        viewModel.onFrameResult(ear, status)
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupCharts() {
        // Line Chart setup
        with(binding.lineChart) {
            setBackgroundColor(Color.TRANSPARENT)
            description.isEnabled = false
            legend.textColor = Color.WHITE
            setTouchEnabled(false)
            axisRight.isEnabled = false
            xAxis.textColor = Color.WHITE
            xAxis.setDrawGridLines(false)
            axisLeft.textColor = Color.WHITE
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 0.6f
            axisLeft.addLimitLine(
                LimitLine(0.25f, "Threshold").apply {
                    lineColor = Color.parseColor("#FF3B5C")
                    lineWidth = 2f
                    textColor = Color.parseColor("#FF3B5C")
                }
            )
            val dataSet = LineDataSet(mutableListOf(), "EAR").apply {
                color = Color.parseColor("#00D4FF")
                lineWidth = 3f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            data = LineData(dataSet)
            setNoDataText("Start detection to see EAR data")
            setNoDataTextColor(Color.parseColor("#8899BB"))
        }

        // Pie Chart setup
        with(binding.pieChart) {
            setBackgroundColor(Color.TRANSPARENT)
            description.isEnabled = false
            legend.textColor = Color.WHITE
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 55f
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            val entries = mutableListOf(PieEntry(1f, "Awake"), PieEntry(0f, "Drowsy"))
            val dataSet = PieDataSet(entries, "").apply {
                colors = listOf(
                    Color.parseColor("#00FF99"),
                    Color.parseColor("#FF3B5C")
                )
                valueTextColor = Color.WHITE
                valueTextSize = 12f
            }
            data = PieData(dataSet)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: DetectionState) {
        val b = _binding ?: return   // guard against updates after view is destroyed

        // Status card
        b.tvStatus.text = state.status
        b.tvEar.text = String.format("%.3f", state.ear)
        b.tvAlerts.text = state.alerts.toString()

        // Status color
        val statusColor = when (state.status) {
            "DROWSY" -> Color.parseColor("#FF3B5C")
            "AWAKE"  -> Color.parseColor("#00FF99")
            else     -> Color.parseColor("#00D4FF")
        }
        b.tvStatus.setTextColor(statusColor)

        // Toggle button — use backgroundTintList for MaterialButton (setBackgroundColor doesn't work)
        if (state.isRunning) {
            b.btnToggle.text = "STOP DETECTION"
            b.btnToggle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF3B5C"))
            b.liveBadge.visibility = View.VISIBLE
        } else {
            b.btnToggle.text = "START DETECTION"
            b.btnToggle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00D4FF"))
            b.liveBadge.visibility = View.INVISIBLE
        }

        // Update line chart
        if (state.isRunning && state.earHistory.isNotEmpty()) {
            frameIndex++
            val dataSet = b.lineChart.data?.getDataSetByIndex(0) as? LineDataSet
            dataSet?.let {
                it.addEntry(Entry(frameIndex.toFloat(), state.ear))
                if (it.entryCount > 30) it.removeFirst()
                b.lineChart.data.notifyDataChanged()
                b.lineChart.notifyDataSetChanged()
                b.lineChart.invalidate()
            }
        }

        // Update pie chart — recreate entries instead of calling clear() which can crash
        val pieData = b.pieChart.data ?: return
        val pieDataSet = pieData.getDataSetByIndex(0) as? PieDataSet ?: return
        val newEntries = mutableListOf(
            PieEntry(maxOf(state.awakeCount.toFloat(), 0f), "Awake"),
            PieEntry(maxOf(state.drowsyCount.toFloat(), 0f), "Drowsy")
        )
        pieDataSet.values = newEntries
        pieData.notifyDataChanged()
        b.pieChart.notifyDataSetChanged()
        b.pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        cameraStarted = false
        _binding = null
    }
}
