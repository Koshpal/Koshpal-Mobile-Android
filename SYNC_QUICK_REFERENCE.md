# Transaction Sync - Quick Reference Guide

## 🚀 Quick Start

### For Testing

1. **Open the app** → Navigate to Profile tab
2. **Click "Sync to Cloud"** button
3. **Watch the progress** → Status will update in real-time
4. **Check the count** → "Total Messages Stored" shows synced transactions

### Key Information

- **Employee ID**: `68ee28ce2f3fd392ea436576` (Static, no login needed)
- **API Base URL**: `https://koshpal-server-wc9o.onrender.com/`
- **Background Sync**: Every 15 minutes automatically

---

## 📱 User Flow

### Initial Setup (First Time)
```
App Launch → Periodic sync scheduled → Profile tab → Click "Sync to Cloud" → All transactions uploaded
```

### Daily Usage
```
New SMS arrives → Transaction created → Auto-synced in background (within 15 mins)
```

---

## 🔧 How to Use APIs

### Bulk Upload (Initial Sync)
```kotlin
// Automatically called from ProfileFragment
profileViewModel.performInitialSync()
```

### Single Transaction Upload (New/Updated)
```kotlin
// Automatically triggered when:
// 1. New SMS is processed
// 2. Transaction is updated (category, star, etc.)
TransactionSyncScheduler.scheduleSingleTransactionSync(context, transactionId)
```

### Manual Immediate Sync
```kotlin
TransactionSyncScheduler.scheduleImmediateSync(context)
```

---

## 📊 Checking Sync Status

### From ProfileFragment UI
- **Status**: Current state (Idle/Syncing/Success/Error)
- **Count**: Total messages backed up
- **Last Sync**: Timestamp of last successful sync
- **Error**: Detailed error message if failed

### From Code
```kotlin
// Get sync count
val count = userPreferences.getTotalSyncedCount()

// Get last error
val error = userPreferences.getLastSyncError()

// Get last sync time
val time = userPreferences.getLastSyncTime()

// Check if initial sync completed
val completed = userPreferences.isInitialSyncCompleted()
```

---

## 🐛 Debugging

### View Logs
```bash
# Filter by sync-related tags
adb logcat | grep -E "TransactionSync|ProfileViewModel|TransactionSyncWorker"
```

### Common Log Messages
| Emoji | Meaning | Example |
|-------|---------|---------|
| 🚀 | Starting operation | "🚀 Starting bulk sync..." |
| ✅ | Success | "✅ Transaction synced successfully" |
| ❌ | Error | "❌ Sync failed: No network" |
| 📊 | Data/Stats | "📊 Found 245 transactions" |
| 📤 | Upload | "📤 Uploading transactions..." |
| 🔄 | In Progress | "🔄 Syncing transaction..." |

### Check WorkManager Jobs
```kotlin
// In debug code
val workManager = WorkManager.getInstance(context)
val workInfos = workManager.getWorkInfosByTag(TransactionSyncWorker.TAG).get()
workInfos.forEach { 
    Log.d("WorkManager", "State: ${it.state}, RunAttemptCount: ${it.runAttemptCount}")
}
```

---

## ⚠️ Common Issues

### Issue: Sync fails with "No network connection"
**Solution**: Check device internet connectivity

### Issue: Sync button disabled
**Solution**: Sync is already in progress, wait for completion

### Issue: Error "API error: 400"
**Solution**: Check request body format, verify employee ID is correct

### Issue: Transactions not syncing automatically
**Solution**: 
1. Check if periodic sync is scheduled (should be in Application.onCreate())
2. Verify WorkManager is running
3. Check battery optimization settings

### Issue: Count shows 0 after successful sync
**Solution**: Refresh ProfileFragment or restart app

---

## 📋 API Request Examples

### Bulk Upload Request
```json
POST /api/transactions/bulk

{
  "employeeId": "68ee28ce2f3fd392ea436576",
  "transactions": [
    {
      "transaction_id": "txn123",
      "sender": "HDFC",
      "message_body": "Rs. 500 debited from account",
      "amount": 500.0,
      "currency": "INR",
      "txn_type": "DEBIT",
      "timestamp_ms": 1704067200000,
      "account_last4": null,
      "merchant": "Amazon",
      "category_id": "SHOPPING",
      "category_name": "Shopping",
      "upi_ref": null,
      "bank": "HDFC",
      "is_starred": false,
      "include_in_cash_flow": false,
      "source": "sms",
      "app_version": "1.0.0",
      "device_id": "abc123"
    }
  ]
}
```

### Single Upload Request
```json
POST /api/transactions/

{
  "employeeId": "68ee28ce2f3fd392ea436576",
  "transaction_id": "txn456",
  "sender": "SBI",
  "message_body": "Rs. 1000 credited to account",
  "amount": 1000.0,
  "currency": "INR",
  "txn_type": "CREDIT",
  "timestamp_ms": 1704067200000,
  "merchant": "Salary",
  "category_id": "INCOME",
  "bank": "SBI",
  "is_starred": false,
  "source": "sms",
  "app_version": "1.0.0",
  "device_id": "abc123"
}
```

---

## 🎯 Testing Checklist

### Basic Functionality
- [ ] App opens without login
- [ ] Profile tab displays sync status
- [ ] Sync button triggers bulk upload
- [ ] Progress bar shows during sync
- [ ] Success message appears after sync
- [ ] Count updates correctly

### Background Sync
- [ ] New SMS creates transaction
- [ ] Transaction auto-syncs in background
- [ ] Periodic sync runs every 15 minutes
- [ ] Updated transactions sync automatically

### Error Handling
- [ ] No network error displays correctly
- [ ] API errors show user-friendly messages
- [ ] Retry button works after error
- [ ] Error persists across app restarts

### Edge Cases
- [ ] Empty transaction list handled gracefully
- [ ] Large transaction list (1000+) syncs successfully
- [ ] App killed during sync recovers correctly
- [ ] Duplicate transaction IDs handled properly

---

## 🔐 Security Notes

- Static employee ID is used (no sensitive user data)
- All API calls use HTTPS
- Local database is the source of truth (no data overwritten from backend)
- No authentication tokens stored

---

## 📞 Quick Commands

### Trigger Sync from Code
```kotlin
// In any Activity/Fragment with context
profileViewModel.performInitialSync()
```

### Check Sync State
```kotlin
viewModel.syncStatus.observe(this) { status ->
    when (status) {
        SyncStatus.IDLE -> // Not syncing
        SyncStatus.SYNCING -> // In progress
        SyncStatus.SUCCESS -> // Completed
        SyncStatus.ERROR -> // Failed
    }
}
```

### Clear Sync Error
```kotlin
profileViewModel.clearSyncError()
```

### Force Re-sync
```kotlin
profileViewModel.forceSyncAll()
```

---

## 📈 Performance

| Metric | Value |
|--------|-------|
| Bulk upload (100 txns) | ~5-10 seconds |
| Single transaction | ~1-2 seconds |
| Background sync interval | 15 minutes |
| Network timeout | 30 seconds |
| WorkManager retry | Exponential backoff |

---

## 🎨 UI States

```
[IDLE]
Status: Idle
Button: "Sync to Cloud" (enabled)
Progress: Hidden

[SYNCING]
Status: Syncing...
Button: "Syncing..." (disabled)
Progress: Visible (0-100%)

[SUCCESS]
Status: ✅ All transactions backed up
Button: "Re-sync to Cloud" (enabled)
Progress: Hidden
Toast: "Sync completed successfully!"

[ERROR]
Status: ❌ Sync failed
Error Message: "Error: No network connection"
Button: "Retry Sync" (enabled)
Progress: Hidden
```

---

## 💡 Pro Tips

1. **First sync**: Always do manual sync from Profile tab after app installation
2. **Monitor logs**: Use `grep` to filter sync logs for debugging
3. **Test offline**: Turn off network to verify error handling
4. **Check backend**: Verify transactions appear in MongoDB after sync
5. **Force sync**: Use immediate sync for testing without waiting 15 minutes

---

## 📚 Related Files

| Component | File Path |
|-----------|-----------|
| Sync Service | `service/TransactionSyncService.kt` |
| View Model | `ui/profile/ProfileViewModel.kt` |
| UI Fragment | `ui/profile/ProfileFragment.kt` |
| Worker | `service/TransactionSyncWorker.kt` |
| Scheduler | `service/TransactionSyncScheduler.kt` |
| API Service | `network/ApiService.kt` |
| DTOs | `data/remote/dto/` |
| Constants | `utils/Constants.kt` |

---

## 🚦 Status Indicators

| Color/Icon | Meaning |
|------------|---------|
| 🔵 Idle | No sync in progress |
| 🟡 Syncing | Sync in progress |
| 🟢 Success | Sync completed successfully |
| 🔴 Error | Sync failed with error |

---

**Last Updated**: January 2024  
**Version**: 1.0.0

