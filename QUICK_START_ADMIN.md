# Quick Start: Admin Account Setup

## 🚀 Fastest Way to Create Admin Account

### Option 1: Use Debug Screen (Easiest - One Click!) ⚡

1. **Add Debug Route** to your MainActivity/Navigation temporarily:

```kotlin
import com.pixelmarket.app.presentation.debug.AdminSetupScreen

// In your NavHost, add this route:
composable("debug/admin-setup") {
    AdminSetupScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

2. **Navigate to Debug Screen**:
   - Add a temporary button somewhere in your app:
   ```kotlin
   Button(onClick = { navController.navigate("debug/admin-setup") }) {
       Text("Admin Setup (Debug)")
   }
   ```
   - Or navigate directly in code

3. **Create Admin Account**:
   - Open the debug screen
   - Click "Create Admin Account"
   - Wait for success message ✅
   - Click "Verify Admin Account" to confirm

4. **Login**:
   - **Email**: `pixelMadmin@gmail.com`
   - **Password**: `admin1234`

5. **Done!** Access Admin Panel from Profile

6. **⚠️ IMPORTANT**: Remove the debug route before production!

---

### Option 2: Manual Setup (Firebase Console)

See `ADMIN_ACCOUNT_SETUP.md` for detailed instructions.

---

## Default Admin Credentials

```
Email:    pixelMadmin@gmail.com
Password: admin1234
```

**⚠️ Change this password after first login!**

---

## Testing Admin Features

After logging in with admin credentials:

1. ✅ Go to **Profile**
2. ✅ You should see **"Admin Panel"** option
3. ✅ Click it to access:
   - 📊 Dashboard (statistics)
   - 👥 User Management
   - 🎨 Theme Customization
   - 📦 Asset Management

---

## Security Checklist

Before deploying to production:

- [ ] Remove debug screen route
- [ ] Remove "Admin Setup (Debug)" button
- [ ] Delete `AdminSetupScreen.kt` file (optional)
- [ ] Change default admin password
- [ ] Update Firestore security rules
- [ ] Audit admin user list
- [ ] Enable logging for admin actions

---

## Troubleshooting

### Problem: "Admin Panel" not showing
**Solution**: 
- Logout and login again
- Check Firestore: `users/{uid}` should have `isAdmin: true`
- Verify with debug screen's "Verify Admin Account" button

### Problem: Account already exists
**Solution**: 
- The setup is safe to run multiple times
- Use Firebase Console to reset password if needed
- Or use "Forgot Password" in the app

### Problem: Can't access admin features
**Solution**: 
- Check Firestore security rules (see `ADMIN_ACCOUNT_SETUP.md`)
- Ensure user document has both `isAdmin: true` AND `role: "admin"`

---

## Files Created

1. ✅ `AdminSetup.kt` - Utility class for account creation
2. ✅ `AdminSetupScreen.kt` - Debug screen (UI)
3. ✅ `ADMIN_ACCOUNT_SETUP.md` - Detailed setup guide
4. ✅ `QUICK_START_ADMIN.md` - This file

---

## Example: Adding Debug Button (Temporary)

Add this to your ProfileScreen or any screen:

```kotlin
// DEBUG ONLY - Remove before production
if (BuildConfig.DEBUG) {
    Card(
        onClick = { navController.navigate("debug/admin-setup") },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Admin Setup (Debug)",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
```

---

## Summary

1. **Add debug route** → 2. **Click "Create Admin Account"** → 3. **Login** → 4. **Access Admin Panel** → 5. **Remove debug code**

That's it! 🎉

---

**Next**: See `ADMIN_INTEGRATION_GUIDE.md` for full admin panel integration.
