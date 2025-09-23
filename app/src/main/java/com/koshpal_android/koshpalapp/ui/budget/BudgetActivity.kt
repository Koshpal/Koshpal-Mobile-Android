package com.koshpal_android.koshpalapp.ui.budget

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshpal_android.koshpalapp.ui.budget.adapter.BudgetEditCategoryAdapter
import com.koshpal_android.koshpalapp.databinding.ActivityBudgetBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private val viewModel: BudgetEditViewModel by viewModels()
    private lateinit var adapter: BudgetEditCategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupList()
        setupClicks()
        viewModel.load()

        viewModel.uiState.observe(this) { state ->
            binding.etTotalBudget.setText(state.totalBudgetText)
            adapter.submitList(state.categories)
            binding.tvValidation.text = state.error ?: ""
        }
    }

    private fun setupList() {
        adapter = BudgetEditCategoryAdapter { name, value ->
            viewModel.updateCategoryValue(name, value)
        }
        binding.rvEditCategories.layoutManager = LinearLayoutManager(this)
        binding.rvEditCategories.adapter = adapter
    }

    private fun setupClicks() {
        binding.btnAutoDistribute.setOnClickListener {
            viewModel.autoDistribute()
        }
        binding.btnSave.setOnClickListener {
            viewModel.save(binding.etTotalBudget.text.toString()) { finish() }
        }
    }
}


