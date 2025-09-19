package com.koshpal_android.koshpalapp.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.koshpal_android.koshpalapp.databinding.FragmentAlertsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertsViewModel by viewModels()
    private lateinit var activeAlertsAdapter: ActiveAlertsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Load initial data
        viewModel.loadAlertSettings()
        viewModel.loadActiveAlerts()
    }

    private fun setupRecyclerView() {
        activeAlertsAdapter = ActiveAlertsAdapter { alert ->
            // Handle alert click - maybe navigate to budget details
            viewModel.onActiveAlertClick(alert)
        }

        binding.rvActiveAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeAlertsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // Alert preference switches
            switchEnableAlerts.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAlertsEnabled(isChecked)
                updateSwitchesEnabled(isChecked)
            }

            switchAlert50.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setThresholdAlert(50, isChecked)
            }

            switchAlert80.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setThresholdAlert(80, isChecked)
            }

            switchAlert100.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setThresholdAlert(100, isChecked)
            }

            // Summary notification switches
            switchDailySummary.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDailySummaryEnabled(isChecked)
            }

            switchWeeklySummary.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setWeeklySummaryEnabled(isChecked)
            }

            // Test notification button
            btnTestNotification.setOnClickListener {
                viewModel.sendTestNotification()
            }
        }
    }

    private fun updateSwitchesEnabled(enabled: Boolean) {
        binding.apply {
            switchAlert50.isEnabled = enabled
            switchAlert80.isEnabled = enabled
            switchAlert100.isEnabled = enabled
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.alertSettings.collect { settings ->
                updateUI(settings)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeAlerts.collect { alerts ->
                activeAlertsAdapter.submitList(alerts)
                binding.tvActiveAlertsCount.text = alerts.size.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvents.collect { event ->
                when (event) {
                    is AlertsUiEvent.ShowMessage -> {
                        Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                    }
                    is AlertsUiEvent.TestNotificationSent -> {
                        Snackbar.make(
                            binding.root, 
                            "Test notification sent!", 
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateUI(settings: AlertSettings) {
        binding.apply {
            // Update switches without triggering listeners
            switchEnableAlerts.setOnCheckedChangeListener(null)
            switchAlert50.setOnCheckedChangeListener(null)
            switchAlert80.setOnCheckedChangeListener(null)
            switchAlert100.setOnCheckedChangeListener(null)
            switchDailySummary.setOnCheckedChangeListener(null)
            switchWeeklySummary.setOnCheckedChangeListener(null)

            switchEnableAlerts.isChecked = settings.alertsEnabled
            switchAlert50.isChecked = settings.threshold50Enabled
            switchAlert80.isChecked = settings.threshold80Enabled
            switchAlert100.isChecked = settings.threshold100Enabled
            switchDailySummary.isChecked = settings.dailySummaryEnabled
            switchWeeklySummary.isChecked = settings.weeklySummaryEnabled

            updateSwitchesEnabled(settings.alertsEnabled)

            // Restore listeners
            setupClickListeners()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class AlertSettings(
    val alertsEnabled: Boolean = true,
    val threshold50Enabled: Boolean = true,
    val threshold80Enabled: Boolean = true,
    val threshold100Enabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true,
    val weeklySummaryEnabled: Boolean = false
)

sealed class AlertsUiEvent {
    data class ShowMessage(val message: String) : AlertsUiEvent()
    object TestNotificationSent : AlertsUiEvent()
}
