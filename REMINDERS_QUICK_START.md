# 🚀 Payment Reminders - Quick Start Guide

## ⚡ Quick Test Steps

### 1️⃣ **Build and Run**
```bash
# Clean and rebuild
./gradlew clean build

# Or in Android Studio:
Build → Clean Project
Build → Rebuild Project
```

### 2️⃣ **Test Basic Flow**

**Step 1: Access Reminders**
- Launch app
- Go to Home screen
- Click "🔔 Reminders" button (next to "Add Transaction")

**Step 2: Create a Test Reminder**
- Click FAB (+) button
- Select type: "💸 I Need to Pay"
- Enter:
  - Person Name: "Test Person"
  - Contact: "+91 9876543210" (optional)
  - Amount: "500"
  - Purpose: "Test payment"
- Select date: Tomorrow
- Select time: Current time + 2 minutes (for quick testing)
- Priority: Medium
- Repeat: None
- Click "Save Reminder"

**Step 3: Verify List**
- You should see the reminder in the list
- Check "Next Payment Due" card shows your reminder
- Statistics should show: 1 pending, ₹500 to pay

**Step 4: Test Notification (2 minutes later)**
- Wait for scheduled time
- Notification should appear with:
  - Title: "💰 Reminder: Pay ₹500"
  - Message showing person name and purpose
  - Two buttons: "Mark Paid" and "Snooze 1hr"

**Step 5: Test Actions**
- Click "Mark Paid" → Reminder marked complete
- OR Click "Snooze 1hr" → Notification reappears in 1 hour

---

## 🔍 Testing Checklist

### ✅ UI Tests
- [ ] Home screen shows both buttons (50% width each)
- [ ] Reminders button opens list screen
- [ ] FAB opens create form
- [ ] Type chips change color when selected
- [ ] Date picker works
- [ ] Time picker works
- [ ] All chips are selectable
- [ ] Save button creates reminder
- [ ] Back navigation works

### ✅ Data Tests
- [ ] Reminder appears in list after creation
- [ ] Statistics update correctly
- [ ] Edit works (click edit icon)
- [ ] Delete works (click delete icon)
- [ ] Mark complete works (click checkmark)

### ✅ Notification Tests
- [ ] Notification appears at scheduled time
- [ ] Notification shows correct information
- [ ] "Mark Paid" completes reminder
- [ ] "Snooze 1hr" reschedules
- [ ] Clicking notification opens app

---

## 🎯 Test Scenarios

### Scenario 1: Payment You Need to Make
```
Type: 💸 I Need to Pay
Person: Rahul
Amount: ₹1000
Purpose: Dinner bill split
Date: Tomorrow
Time: 6:00 PM
Priority: High
```

### Scenario 2: Payment You Should Receive
```
Type: 💰 Someone Owes Me
Person: Priya
Amount: ₹2500
Purpose: Loan repayment
Date: Next Monday
Time: 10:00 AM
Priority: Medium
```

### Scenario 3: Recurring Monthly Rent
```
Type: 💸 I Need to Pay
Person: Landlord
Amount: ₹15000
Purpose: Monthly rent
Date: 1st of next month
Time: 9:00 AM
Priority: High
Repeat: Monthly
```

---

## 🐛 Troubleshooting

### Issue: Build Error
**Solution**: 
```bash
# Invalidate cache and restart
File → Invalidate Caches / Restart → Invalidate and Restart
```

### Issue: Database Error
**Solution**: Uninstall and reinstall app (database will be recreated with v8)
```bash
adb uninstall com.koshpal_android.koshpalapp
# Then run app again from Android Studio
```

### Issue: Notification Not Appearing
**Solutions**:
1. Check notification permission is granted
2. Check battery optimization is disabled for app
3. For Android 12+: Check "Alarms & reminders" permission
4. Check logcat for errors:
   ```
   Filter: ReminderNotification
   ```

### Issue: Exact Alarm Permission (Android 12+)
**Solution**: 
- Go to Settings → Apps → Koshpal → Alarms & reminders
- Enable "Allow setting alarms and reminders"

---

## 📱 Permissions Needed

### Grant these permissions when prompted:
1. ✅ **Notifications** - For showing reminders
2. ✅ **Alarms & Reminders** (Android 12+) - For exact timing
3. ⚠️ **Battery Optimization** - Disable for reliable notifications

### To manually grant:
```
Settings → Apps → Koshpal → Permissions
- Notifications: Allow
- Alarms & reminders: Allow

Settings → Apps → Koshpal → Battery
- Battery optimization: Don't optimize
```

---

## 📊 Expected Behavior

### On Create:
- Reminder saved to database ✅
- Notification scheduled ✅
- Appears in list immediately ✅
- Statistics updated ✅

### On Notification Time:
- Notification appears ✅
- Shows rich content ✅
- Action buttons work ✅
- Clicking opens app ✅

### On Mark Complete:
- Status changes to COMPLETED ✅
- Notification cancelled ✅
- Appears in completed list ✅
- Statistics updated ✅

---

## 🎨 UI Elements to Verify

### Reminders List Screen:
1. Back button (top left)
2. Search button (top right)
3. Three statistics cards (pending, to pay, to receive)
4. "Next Payment Due" card (gradient background)
5. "All Reminders" section header
6. Reminder cards with:
   - Type badge (colored)
   - Person name
   - Amount (colored by type)
   - Purpose
   - Date and time
   - Priority badge (conditional)
   - Action buttons
7. FAB button (bottom right)

### Set Reminder Screen:
1. Back button
2. Title (changes to "Edit Reminder" when editing)
3. Type selection chips
4. All input fields with icons
5. Date and time selection buttons
6. Repeat chips
7. Priority chips
8. Cancel and Save buttons

---

## 📝 Logcat Tags for Debugging

Filter logcat with these tags:
```
ReminderViewModel
RemindersListFragment
SetReminderFragment
ReminderNotificationHelper
ReminderBroadcastReceiver
ReminderActionReceiver
HomeFragment (for button click)
HomeActivity (for navigation)
```

Example logcat command:
```bash
adb logcat -s ReminderViewModel ReminderBroadcastReceiver
```

---

## ✨ Demo Data

Create these 3 reminders for a nice demo:

**1. Urgent Payment (Today)**
- Type: GIVE
- Person: "Electrician"
- Amount: 800
- Purpose: "Repair work payment"
- Time: 30 minutes from now
- Priority: HIGH

**2. Upcoming Payment (Tomorrow)**
- Type: GIVE
- Person: "Internet Provider"
- Amount: 1200
- Purpose: "Monthly internet bill"
- Time: 6:00 PM
- Priority: MEDIUM

**3. Money to Receive (Next Week)**
- Type: RECEIVE
- Person: "Office Colleague"
- Amount: 5000
- Purpose: "Trip expenses reimbursement"
- Time: 10:00 AM
- Priority: LOW

---

## 🎉 Success Indicators

✅ **Implementation is working if:**
1. Home screen has 2 buttons side by side
2. Clicking Reminders opens list
3. Can create/edit/delete reminders
4. Statistics show correct values
5. Notifications appear on time
6. Action buttons work in notification
7. No crashes or errors in logcat

---

## 📞 Quick Support

**Common Error Messages:**

| Error | Cause | Solution |
|-------|-------|----------|
| "Cannot schedule exact alarm" | Android 12+ permission | Grant alarm permission |
| "Database migration failed" | Old database version | Uninstall & reinstall |
| "Notification permission denied" | Not granted | Grant in app settings |
| "Reminder not found" | Database issue | Check logcat for errors |

---

## 🔥 Pro Tips

1. **Quick Testing**: Set reminders for 1-2 minutes in future for fast testing
2. **Multiple Reminders**: Create several to test list scrolling
3. **Edge Cases**: Try past dates, very large amounts, special characters
4. **Performance**: Create 50+ reminders to test RecyclerView performance
5. **Notifications**: Test with screen off, app closed, different times

---

## 🚀 Ready to Test!

Your payment reminders system is fully implemented and ready to use. Start with the basic flow above, then explore all features. Check the comprehensive documentation in `PAYMENT_REMINDERS_IMPLEMENTATION.md` for detailed technical information.

**Happy Testing! 🎯**
