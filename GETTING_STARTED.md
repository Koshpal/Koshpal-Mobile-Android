# Getting Started with Transaction Sync

## 🚀 Quick Start (3 Steps)

### 1. Build the Project
```bash
# Sync Gradle dependencies
./gradlew build
```

### 2. Run the App
```bash
# Install on connected device/emulator
./gradlew installDebug
```

### 3. Test the Sync
1. Open the app
2. Navigate to **Profile** tab (bottom navigation)
3. Click **"Sync to Cloud"** button
4. Watch the status update in real-time

---

## ✅ Pre-flight Checklist

Before testing, ensure:

- [ ] **Backend server is running** at `https://koshpal-server-wc9o.onrender.com/`
- [ ] **Internet connection** is available on device
- [ ] **App has SMS permissions** (for reading transaction SMS)
- [ ] **Some transactions exist** in the local database (send test SMS if needed)

---

## 📱 What to Expect

### On First Launch

1. **App opens** without login screen ✓
2. **Background sync scheduled** automatically (every 15 minutes) ✓
3. **Profile tab** shows:
   ```
   Status: Idle
   Total Messages Stored: 0
   Last Sync: Never
   ```

### After First Sync

1. Click **"Sync to Cloud"** button
2. See progress:
   ```
   Status: Syncing...
   [Progress bar shows 0-100%]
   Button: "Syncing..." (disabled)
   ```
3. On success:
   ```
   Status: ✅ All transactions backed up
   Total Messages Stored: [count]
   Last Sync: [current time]
   Button: "Re-sync to Cloud" (enabled)
   ```

### Background Sync (Automatic)

- **New SMS** → Transaction created → Auto-synced within 1 minute
- **Transaction updated** → Auto-synced within 1 minute
- **Periodic sync** → Runs every 15 minutes automatically

---

## 🧪 Testing Scenarios

### Test 1: Initial Bulk Sync
```
1. Open app
2. Go to Profile tab
3. Click "Sync to Cloud"
4. ✓ Progress bar appears
5. ✓ Status changes to "Syncing..."
6. ✓ Count updates after completion
7. ✓ Toast shows "Sync completed successfully!"
```

### Test 2: Background Sync
```
1. Send test SMS transaction
2. ✓ Transaction appears in Transactions tab
3. Wait 1 minute (or use immediate sync)
4. ✓ Check backend - transaction should be there
5. Go to Profile tab
6. ✓ Count increases by 1
```

### Test 3: Error Handling
```
1. Turn off internet
2. Click "Sync to Cloud"
3. ✓ Error message appears
4. ✓ Button changes to "Retry Sync"
5. Turn on internet
6. Click "Retry Sync"
7. ✓ Sync completes successfully
```

---

## 📊 Verify Backend Data

### Using MongoDB Compass
1. Connect to your MongoDB instance
2. Navigate to your database
3. Open `transactions` collection
4. Look for documents with `employeeId: "68ee28ce2f3fd392ea436576"`
5. Verify `transaction_id`, `amount`, `merchant`, etc.

### Using API Testing Tool (Postman/Insomnia)
```
GET https://koshpal-server-wc9o.onrender.com/api/transactions?employeeId=68ee28ce2f3fd392ea436576
```

---

## 🐛 Troubleshooting

### Issue: Button stays disabled
**Solution**: Check logcat for errors, may need to restart app

### Issue: Count shows 0 after sync
**Solution**: 
```bash
# Check logs
adb logcat | grep "TransactionSyncService"

# Look for success message
"✅ Bulk sync successful: X transactions inserted"
```

### Issue: Background sync not working
**Solution**:
```bash
# Check WorkManager status
adb shell dumpsys jobscheduler | grep TransactionSyncWorker

# Or force immediate sync from code:
TransactionSyncScheduler.scheduleImmediateSync(context)
```

### Issue: API returns 400/500 error
**Solution**:
- Verify backend is running
- Check request body format in logs
- Verify employee ID is correct
- Check backend logs for errors

---

## 📝 View Logs

### Android Studio Logcat
Filter by tags:
```
TransactionSync
ProfileViewModel
TransactionSyncWorker
TransactionProcessingService
```

### Command Line
```bash
# All sync logs
adb logcat | grep -E "TransactionSync|ProfileViewModel|TransactionSyncWorker"

# Only errors
adb logcat *:E | grep TransactionSync

# Specific component
adb logcat TransactionSyncService:D *:S
```

---

## 🎯 Key Features to Demo

### 1. No Login Required
✓ App opens directly without login screen  
✓ Static employee ID used for all requests

### 2. Manual Sync
✓ Profile tab has "Sync to Cloud" button  
✓ Real-time progress indication  
✓ Success/error feedback

### 3. Automatic Background Sync
✓ New transactions sync automatically  
✓ Updated transactions re-sync  
✓ Periodic sync every 15 minutes

### 4. Comprehensive Status Display
✓ Current sync state  
✓ Total messages backed up  
✓ Last sync timestamp  
✓ Error messages (if any)

### 5. Local Data Priority
✓ UI uses local database  
✓ API responses don't update UI  
✓ Sync is for backup only

---

## 🔄 Daily Workflow

### For End Users
```
1. Use app normally
2. Transactions sync automatically in background
3. Occasionally check Profile tab for status
4. All done! No manual intervention needed
```

### For Developers/Testers
```
1. Monitor logcat for sync activity
2. Verify backend receives data
3. Test error scenarios
4. Check WorkManager jobs
5. Review sync statistics
```

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| `IMPLEMENTATION_SUMMARY.md` | Overview of what was implemented |
| `SYNC_IMPLEMENTATION.md` | Detailed technical documentation |
| `SYNC_QUICK_REFERENCE.md` | Quick reference guide |
| `GETTING_STARTED.md` | This file - getting started guide |

---

## 🎓 Code Examples

### Trigger Sync Programmatically
```kotlin
// In Activity/Fragment
val viewModel: ProfileViewModel by viewModels()
viewModel.performInitialSync()
```

### Observe Sync Status
```kotlin
viewModel.syncStatus.observe(this) { status ->
    when (status) {
        SyncStatus.IDLE -> // Ready to sync
        SyncStatus.SYNCING -> // In progress
        SyncStatus.SUCCESS -> // Completed
        SyncStatus.ERROR -> // Failed
    }
}
```

### Check Sync Count
```kotlin
@Inject lateinit var userPreferences: UserPreferences

val count = userPreferences.getTotalSyncedCount()
Log.d("Sync", "Total synced: $count")
```

### Force Immediate Sync
```kotlin
TransactionSyncScheduler.scheduleImmediateSync(context)
```

---

## 🎨 UI Screenshots Expectations

### Profile Screen - Before Sync
```
┌─────────────────────────────────┐
│ Profile                         │
├─────────────────────────────────┤
│ User Information                │
│ Email: koshpal.user@app.com     │
│ Employee ID: 68ee28ce...        │
│ ✅ App Registered               │
├─────────────────────────────────┤
│ Data Backup Status              │
│ Status: Idle                    │
│ Total Messages Stored: 0        │
│ Last Sync: Never                │
├─────────────────────────────────┤
│ [Sync to Cloud]                 │
└─────────────────────────────────┘
```

### Profile Screen - After Sync
```
┌─────────────────────────────────┐
│ Profile                         │
├─────────────────────────────────┤
│ User Information                │
│ Email: koshpal.user@app.com     │
│ Employee ID: 68ee28ce...        │
│ ✅ App Registered               │
├─────────────────────────────────┤
│ Data Backup Status              │
│ Status: ✅ All transactions     │
│         backed up               │
│ Total Messages Stored: 245      │
│ Last Sync: Jan 15, 2024 10:30 AM│
├─────────────────────────────────┤
│ [Re-sync to Cloud]              │
└─────────────────────────────────┘
```

---

## ⚡ Performance Tips

1. **First sync may take longer** with many transactions (10-30 seconds for 1000 txns)
2. **Background sync is silent** - no user notification
3. **Network errors auto-retry** with exponential backoff
4. **Battery optimized** - uses WorkManager constraints

---

## 🔐 Security Notes

- Static employee ID is safe (no user-specific data exposed)
- All requests use HTTPS
- No authentication tokens stored
- Backend validates all requests
- Local database encrypted (if Room encryption enabled)

---

## 🎉 Success Indicators

You'll know it's working when:

✅ Profile tab shows non-zero count after sync  
✅ Backend database contains transactions with correct employeeId  
✅ Logs show "✅ Bulk sync successful: X transactions inserted"  
✅ New SMS transactions auto-sync without user intervention  
✅ Status updates in real-time during sync  

---

## 📞 Need Help?

1. **Check logs first** - Most issues show clear error messages
2. **Verify network** - Ensure device has internet
3. **Check backend** - Verify API server is accessible
4. **Review documentation** - See `SYNC_IMPLEMENTATION.md` for details
5. **Test incremental** - Start with small transaction count

---

## 🚀 Ready to Go!

You're all set! The implementation is complete and ready for testing.

**Next Steps:**
1. Build and run the app
2. Navigate to Profile tab
3. Click "Sync to Cloud"
4. Watch the magic happen! ✨

---

**Happy Syncing! 🎉**

