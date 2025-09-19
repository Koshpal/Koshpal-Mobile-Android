package com.koshpal_android.koshpalapp.alerts

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.koshpal_android.koshpalapp.repository.BudgetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BudgetCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val spendingAlertManager: SpendingAlertManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Check all active budgets for threshold violations
            val activeBudgets = budgetRepository.getActiveBudgets()
            
            activeBudgets.forEach { budget ->
                val progressPercentage = budget.progressPercentage
                
                when {
                    progressPercentage >= 100f && budget.status.name != "EXCEEDED" -> {
                        // Budget exceeded
                        // This would trigger an alert through the existing system
                    }
                    progressPercentage >= 80f && progressPercentage < 100f -> {
                        // 80% threshold reached
                    }
                    progressPercentage >= 50f && progressPercentage < 80f -> {
                        // 50% threshold reached
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
