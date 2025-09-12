package com.koshpal_android.koshpalapp.ui.payments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.koshpal_android.koshpalapp.databinding.FragmentPaymentsBinding

class PaymentsFragment : Fragment() {

    private var _binding: FragmentPaymentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentsViewModel by viewModels()
    private lateinit var adapter: PaymentSmsAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            loadPaymentMessages()
        } else {
            showPermissionDeniedMessage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        checkPermissionAndLoadData()
    }

    private fun setupRecyclerView() {
        adapter = PaymentSmsAdapter()
        binding.recyclerViewPayments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PaymentsFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.paymentSms.observe(viewLifecycleOwner) { smsMessages ->
            adapter.submitList(smsMessages)
            updateEmptyState(smsMessages.isEmpty())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.apply {
                    text = error
                    visibility = View.VISIBLE
                }
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                    .setAction("Retry") {
                        viewModel.clearError()
                        checkPermissionAndLoadData()
                    }
                    .show()
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
    }

    private fun checkPermissionAndLoadData() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadPaymentMessages()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) -> {
                showPermissionRationale()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }
    }

    private fun loadPaymentMessages() {
        viewModel.loadPaymentSms()
    }

    private fun showPermissionRationale() {
        Snackbar.make(
            binding.root,
            "SMS permission is required to read payment messages",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Grant Permission") {
            requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }.show()
    }

    private fun showPermissionDeniedMessage() {
        binding.tvError.apply {
            text = "SMS permission denied. Cannot read payment messages. Please grant permission in app settings."
            visibility = View.VISIBLE
        }
        updateEmptyState(true)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmptyState.visibility = if (isEmpty && !viewModel.isLoading.value!!) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.recyclerViewPayments.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
