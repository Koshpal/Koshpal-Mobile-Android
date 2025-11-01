# Transaction Sync Implementation Guide

## Overview

This document describes the complete implementation of the data synchronization feature for the Koshpal Android app. The implementation backs up SMS transactions from the local Room database to the Node.js backend using API-based synchronization.

## Key Features

✅ **No Login Required**: Uses a static employee ID for all API requests  
✅ **Bulk Upload**: Initial sync uploads all transactions in one API call  
✅ **Background Sync**: Automatic syncing of new/updated transactions  
✅ **Local-First**: UI uses local database as the single source of truth  
✅ **Comprehensive Status**: Real-time sync status, count, and error tracking  
✅ **WorkManager Integration**: Reliable background sync every 15 minutes  

---

## Architecture

### 1. Static Employee ID
- **No login screen required**
- Uses static employee ID: `68ee28ce2f3fd392ea436576`
- Defined in `Constants.kt`

### 2. API Endpoints

#### Base URL
```
https://koshpal-server-wc9o.onrender.com/
```

#### Bulk Transaction Upload
- **Endpoint**: `POST /api/transactions/bulk`
- **Use Case**: First-time sync of all existing transactions
- **Request Body**:
```json
{
  "employeeId": "68ee28ce2f3fd392ea436576",
  "transactions": [
    {
      "transaction_id": "txnr",
      "sender": "Amazon",
      "message_body": "Rs. 5000 debited...",
      "amount": 10000,
      "currency": "INR",
      "txn_type": "DEBIT",
      "timestamp_ms": 1734273600000,
      ...
    }
  ]
}
```
- **Response**:
```json
{
  "success": true,
  "message": "Processed 3 transactions (inserted: 2)",
  "data": {
    "insertedCount": 2,
    "insertedIds": ["id1", "id2"]
  }
}
```

#### Single Transaction Upload
- **Endpoint**: `POST /api/transactions/`
- **Use Case**: Sync individual new or updated transactions
- **Request Body**: Single `TransactionDto` object
- **Response**:
```json
{
  "success": true,
  "message": "Transaction created successfully",
  "data": {
    "employeeId": "68ee28ce2f3fd392ea436576",
    "transactionId": "t6n4",
    "_id": "69059f258ce962d367bce5a3"
  }
}
```

---

## Implementation Details

### Core Components

#### 1. Data Transfer Objects (DTOs)

**BulkTransactionRequest.kt**
```kotlin
data class BulkTransactionRequest(
    val employeeId: String,
    val transactions: List<TransactionDto>
)
```

**BulkTransactionResponse.kt**
```kotlin
data class BulkTransactionResponse(
    val success: Boolean,
    val message: String,
    val data: BulkTransactionData?
)
```

**SingleTransactionResponse.kt**
```kotlin
data class SingleTransactionResponse(
    val success: Boolean,
    val message: String,
    val data: SingleTransactionData?
)
```

#### 2. API Service

**ApiService.kt**
```kotlin
@POST("api/transactions/bulk")
suspend fun uploadBulkTransactions(
    @Body request: BulkTransactionRequest
): Response<BulkTransactionResponse>

@POST("api/transactions/")
suspend fun uploadSingleTransaction(
    @Body transaction: TransactionDto
): Response<SingleTransactionResponse>
```

#### 3. Sync Service

**TransactionSyncService.kt**
- Uses static employee ID (no user authentication required)
- Provides bulk sync and single transaction sync methods
- Tracks sync state, progress, and errors
- Updates SharedPreferences with sync count and status

Key Methods:
- `performInitialSync()`: Bulk uploads all transactions
- `syncSingleTransaction()`: Syncs a single transaction
- `autoSyncNewTransaction()`: Auto-syncs when new SMS is processed
- `autoSyncTransactionUpdate()`: Auto-syncs when transaction is modified

#### 4. User Preferences

**UserPreferences.kt**
New methods added:
```kotlin
fun getTotalSyncedCount(): Long
fun setTotalSyncedCount(count: Long)
fun incrementSyncedCount(count: Int = 1)
fun getLastSyncError(): String?
fun setLastSyncError(error: String?)
fun getLastSyncTime(): Long
fun setLastSyncTime(time: Long)
```

#### 5. ViewModel

**ProfileViewModel.kt**
Exposes LiveData for:
- `syncStatus`: Current sync state (IDLE, SYNCING, SUCCESS, ERROR)
- `totalSyncedCount`: Total number of synced transactions
- `lastSyncError`: Last error message (if any)
- `lastSyncTime`: Timestamp of last successful sync
- `syncProgress`: Progress percentage (0-100)

#### 6. UI Fragment

**ProfileFragment.kt**
Displays:
- Current sync status
- Total messages stored in cloud
- Last sync time
- Error messages (if any)
- Sync progress bar during syncing
- Sync button to manually trigger sync

#### 7. Background Sync

**TransactionSyncWorker.kt**
- WorkManager worker for background sync
- Supports single transaction sync and bulk sync
- Runs with network connectivity constraint

**TransactionSyncScheduler.kt**
- Schedules periodic sync every 15 minutes
- Schedules immediate sync for new/updated transactions
- Manages WorkManager work requests

**Integration Points:**
- `Application.onCreate()`: Schedules periodic background sync
- `TransactionProcessingService`: Triggers sync when new SMS is processed
- Transaction updates: Triggers sync when transaction is modified

---

## Usage

### 1. Initial Bulk Sync

**From ProfileFragment:**
```kotlin
// User clicks "Sync to Cloud" button
profileViewModel.performInitialSync()
```

**What happens:**
1. Service fetches all transactions from local database
2. Converts them to DTOs
3. Calls bulk upload API
4. Updates sync count in SharedPreferences
5. Updates UI with success/error status

### 2. Background Sync

**Automatic triggers:**
- **Periodic**: Every 15 minutes (scheduled in `Application.onCreate()`)
- **New Transaction**: When SMS is processed and transaction is created
- **Updated Transaction**: When transaction category or properties change

**Manual trigger:**
```kotlin
TransactionSyncScheduler.scheduleImmediateSync(context)
```

### 3. Single Transaction Sync

**When a new transaction is detected:**
```kotlin
// In TransactionProcessingService
TransactionSyncScheduler.scheduleSingleTransactionSync(context, transactionId)
```

**When a transaction is updated:**
```kotlin
// After updating transaction in database
transactionRepository.updateTransactionCategory(id, categoryId)
TransactionSyncScheduler.scheduleSingleTransactionSync(context, id)
```

---

## UI Flow

### ProfileFragment Display

```
┌─────────────────────────────────────┐
│      Data Backup Status             │
├─────────────────────────────────────┤
│ Status: ✅ All transactions backed  │
│ Total Messages Stored: 245          │
│ Last Sync: Jan 15, 2024 10:30 AM   │
│                                     │
│ All your SMS transactions are       │
│ automatically backed up to our      │
│ secure cloud database               │
└─────────────────────────────────────┘
│                                     │
│  [Re-sync to Cloud]                │
└─────────────────────────────────────┘
```

### Sync States

| State | UI Display | Button State |
|-------|-----------|--------------|
| IDLE | "Status: Idle" | Enabled: "Sync to Cloud" |
| SYNCING | "Status: Syncing..." + Progress bar | Disabled: "Syncing..." |
| SUCCESS | "Status: ✅ All transactions backed up" | Enabled: "Re-sync to Cloud" |
| ERROR | "Status: ❌ Sync failed" + Error message | Enabled: "Retry Sync" |

---

## Error Handling

### Network Errors
- Checks for network connectivity before sync
- Displays user-friendly error messages
- Stores error in SharedPreferences
- WorkManager automatically retries with exponential backoff

### API Errors
- Logs HTTP status codes and error messages
- Updates UI with error details
- Allows user to retry manually

### Database Errors
- Catches exceptions during transaction fetching
- Logs errors for debugging
- Continues processing other transactions

---

## Testing

### Manual Testing

1. **Test Initial Sync:**
   - Open ProfileFragment
   - Click "Sync to Cloud" button
   - Verify progress bar appears
   - Verify success message and updated count
   - Check backend database for synced transactions

2. **Test Background Sync:**
   - Send a test SMS transaction
   - Wait 15 minutes for periodic sync
   - Or trigger immediate sync
   - Verify transaction appears in backend

3. **Test Error Handling:**
   - Turn off network
   - Try to sync
   - Verify error message displays
   - Turn on network
   - Retry sync

### Debug Logging

All components include comprehensive logging:
```kotlin
Log.d("TransactionSyncService", "🚀 Starting bulk sync...")
Log.d("TransactionSyncService", "✅ Synced ${count} transactions")
Log.e("TransactionSyncService", "❌ Sync failed: ${error}")
```

Filter logs by tags:
- `TransactionSyncService`
- `ProfileViewModel`
- `ProfileFragment`
- `TransactionSyncWorker`
- `TransactionProcessingService`

---

## Important Notes

### Local Data is King
⚠️ **The app UI must always use the local database as the single source of truth.**
- API responses (like summary data) should NOT be used to update app state
- The sync is for backup purposes only
- Local database remains the authoritative data source

### No Authentication Required
- No login screen needed
- Static employee ID is used for all requests
- User registration is not required for sync functionality

### Automatic Backup
- All transactions are automatically backed up
- Users don't need to manually trigger sync (though they can)
- Background sync runs every 15 minutes
- New transactions sync immediately

---

## Dependencies

### Required in build.gradle

```gradle
// WorkManager for background sync
implementation "androidx.work:work-runtime-ktx:2.8.1"
implementation "androidx.hilt:hilt-work:1.0.0"
kapt "androidx.hilt:hilt-compiler:1.0.0"

// Retrofit for API calls (already present)
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.retrofit2:converter-gson:2.9.0"

// Coroutines (already present)
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4"
```

---

## Files Modified/Created

### Created Files
1. `data/remote/dto/BulkTransactionRequest.kt`
2. `data/remote/dto/BulkTransactionResponse.kt`
3. `data/remote/dto/SingleTransactionResponse.kt`
4. `service/TransactionSyncWorker.kt`
5. `service/TransactionSyncScheduler.kt`

### Modified Files
1. `utils/Constants.kt` - Added static employee ID and new base URL
2. `network/ApiService.kt` - Added bulk and single upload endpoints
3. `service/TransactionSyncService.kt` - Updated to use static ID and bulk API
4. `data/local/UserPreferences.kt` - Added sync count and error tracking methods
5. `ui/profile/ProfileViewModel.kt` - Completely rewritten with new sync tracking
6. `ui/profile/ProfileFragment.kt` - Updated to display comprehensive sync info
7. `res/layout/fragment_profile.xml` - Added sync status views
8. `Application.kt` - Added periodic sync scheduling
9. `service/TransactionProcessingService.kt` - Added sync triggers

---

## Next Steps

### Optional Enhancements

1. **Conflict Resolution**: Handle cases where backend data differs from local
2. **Selective Sync**: Allow users to choose which transactions to sync
3. **Data Recovery**: Implement restore from backend feature
4. **Sync Analytics**: Track sync success rates and performance metrics
5. **Push Notifications**: Notify users when sync completes or fails

### Production Checklist

- [ ] Test with large transaction datasets (1000+ transactions)
- [ ] Verify network error handling in various conditions
- [ ] Test battery optimization impact
- [ ] Verify sync works after app is killed
- [ ] Test on different Android versions (API 21-33)
- [ ] Add ProGuard rules if needed for DTOs
- [ ] Set up backend monitoring for sync endpoints
- [ ] Document API rate limits and implement throttling if needed

---

## Support

For issues or questions:
1. Check logs for detailed error messages
2. Verify network connectivity
3. Confirm backend API is accessible
4. Check SharedPreferences for sync state
5. Review WorkManager pending jobs

---

## Summary

This implementation provides a robust, automatic backup system for SMS transactions without requiring user authentication. It uses modern Android best practices including:
- MVVM architecture
- Coroutines for async operations
- WorkManager for reliable background work
- LiveData/StateFlow for reactive UI updates
- Retrofit for type-safe API calls
- Hilt for dependency injection

The system is designed to be resilient, with automatic retries, comprehensive error handling, and user-friendly status displays.

