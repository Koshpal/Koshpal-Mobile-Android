package com.koshpal_android.koshpalapp.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.koshpal_android.koshpalapp.databinding.FragmentInsightsBinding
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var transactionRepository: TransactionRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        loadInsightsData()
    }

    private fun setupUI() {
        binding.apply {
            // Setup any click listeners or UI components
        }
    }

    private fun loadInsightsData() {
        lifecycleScope.launch {
            try {
                // Load insights data
                // This can include spending trends, category analysis, etc.
                
            } catch (e: Exception) {
                android.util.Log.e("InsightsFragment", "Failed to load insights: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
