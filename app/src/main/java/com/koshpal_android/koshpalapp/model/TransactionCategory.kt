package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.koshpal_android.koshpalapp.R

@Entity(tableName = "transaction_categories")
data class TransactionCategory(
    @PrimaryKey
    val id: String,
    val name: String,
    val icon: Int,
    val color: String,
    val keywords: List<String>,
    val isDefault: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getDefaultCategories(): List<TransactionCategory> {
            return listOf(
                TransactionCategory(
                    id = "food",
                    name = "Food & Dining",
                    icon = R.drawable.ic_menu_eat,
                    color = "#FF6B35",
                    keywords = listOf("zomato", "swiggy", "restaurant", "cafe", "food", "dining", "pizza", "burger", "dominos", "kfc", "mcdonalds")
                ),
                TransactionCategory(
                    id = "grocery",
                    name = "Grocery",
                    icon = R.drawable.ic_menu_gallery,
                    color = "#4CAF50",
                    keywords = listOf("bigbasket", "grofers", "blinkit", "zepto", "dmart", "grocery", "supermarket", "vegetables", "fruits")
                ),
                TransactionCategory(
                    id = "transport",
                    name = "Transportation",
                    icon = R.drawable.ic_menu_directions,
                    color = "#2196F3",
                    keywords = listOf("uber", "ola", "metro", "bus", "petrol", "fuel", "taxi", "auto", "rapido", "namma yatri")
                ),
                TransactionCategory(
                    id = "bills",
                    name = "Bills & Utilities",
                    icon = R.drawable.ic_category_default,
                    color = "#FF9800",
                    keywords = listOf("electricity", "water", "gas", "internet", "mobile", "recharge", "broadband", "wifi", "postpaid")
                ),
                TransactionCategory(
                    id = "education",
                    name = "Education",
                    icon = R.drawable.ic_info,
                    color = "#9C27B0",
                    keywords = listOf("fees", "course", "book", "education", "school", "college", "university", "tuition", "coaching")
                ),
                TransactionCategory(
                    id = "entertainment",
                    name = "Entertainment",
                    icon = R.drawable.ic_category_default,
                    color = "#E91E63",
                    keywords = listOf("netflix", "amazon prime", "hotstar", "spotify", "movie", "cinema", "theatre", "gaming", "youtube premium")
                ),
                TransactionCategory(
                    id = "healthcare",
                    name = "Healthcare",
                    icon = R.drawable.ic_category_default,
                    color = "#F44336",
                    keywords = listOf("hospital", "doctor", "medicine", "pharmacy", "medical", "health", "clinic", "apollo", "fortis")
                ),
                TransactionCategory(
                    id = "shopping",
                    name = "Shopping",
                    icon = R.drawable.ic_add,
                    color = "#795548",
                    keywords = listOf("amazon", "flipkart", "myntra", "ajio", "shopping", "clothes", "fashion", "electronics", "gadgets")
                ),
                TransactionCategory(
                    id = "salary",
                    name = "Salary & Income",
                    icon = R.drawable.ic_trending_up,
                    color = "#4CAF50",
                    keywords = listOf("salary", "credited", "income", "bonus", "incentive", "refund", "cashback")
                ),
                TransactionCategory(
                    id = "others",
                    name = "Others",
                    icon = R.drawable.ic_more_vert,
                    color = "#607D8B",
                    keywords = listOf()
                )
            )
        }
    }
}
