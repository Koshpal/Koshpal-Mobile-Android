package com.koshpal_android.koshpalapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.koshpal_android.koshpalapp.R

@Entity(tableName = "transaction_categories")
data class TransactionCategory(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon")
    val icon: Int,
    @ColumnInfo(name = "color")
    val color: String,
    @ColumnInfo(name = "keywords")
    val keywords: List<String>,
    @ColumnInfo(name = "isDefault")
    val isDefault: Boolean = true,
    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getDefaultCategories(): List<TransactionCategory> {
            return listOf(
                TransactionCategory(
                    id = "food",
                    name = "Food & Dining",
                    icon = R.drawable.ic_food_category,
                    color = "#FF9800",
                    keywords = listOf("zomato", "swiggy", "restaurant", "cafe", "food", "dining", "pizza", "burger", "dominos", "kfc", "mcdonalds")
                ),
                TransactionCategory(
                    id = "grocery",
                    name = "Grocery",
                    icon = R.drawable.ic_home_category,
                    color = "#4CAF50",
                    keywords = listOf("bigbasket", "grofers", "blinkit", "zepto", "dmart", "grocery", "supermarket", "vegetables", "fruits")
                ),
                TransactionCategory(
                    id = "transport",
                    name = "Transportation",
                    icon = R.drawable.ic_trending_up,
                    color = "#2196F3",
                    keywords = listOf("uber", "ola", "metro", "bus", "petrol", "fuel", "taxi", "auto", "rapido", "namma yatri")
                ),
                TransactionCategory(
                    id = "bills",
                    name = "Bills & Utilities",
                    icon = R.drawable.ic_note,
                    color = "#FFC107",
                    keywords = listOf("electricity", "water", "gas", "internet", "mobile", "recharge", "broadband", "wifi", "postpaid")
                ),
                TransactionCategory(
                    id = "education",
                    name = "Education",
                    icon = R.drawable.ic_insights,
                    color = "#9C27B0",
                    keywords = listOf("fees", "course", "book", "education", "school", "college", "university", "tuition", "coaching")
                ),
                TransactionCategory(
                    id = "entertainment",
                    name = "Entertainment",
                    icon = R.drawable.ic_entertainment,
                    color = "#E91E63",
                    keywords = listOf("netflix", "amazon prime", "hotstar", "spotify", "movie", "cinema", "theatre", "gaming", "youtube premium")
                ),
                TransactionCategory(
                    id = "healthcare",
                    name = "Healthcare",
                    icon = R.drawable.ic_help,
                    color = "#00BCD4",
                    keywords = listOf("hospital", "doctor", "medicine", "pharmacy", "medical", "health", "clinic", "apollo", "fortis")
                ),
                TransactionCategory(
                    id = "shopping",
                    name = "Shopping",
                    icon = R.drawable.ic_category_default,
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
                    icon = R.drawable.ic_category,
                    color = "#607D8B",
                    keywords = listOf()
                )
            )
        }
    }
}
