package com.saikou.tvlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.saikou.tvlauncher.adapters.AppGridAdapter
import com.saikou.tvlauncher.data.local.LauncherPreferences
import com.saikou.tvlauncher.data.model.AppInfo
import com.saikou.tvlauncher.databinding.ActivityMainBinding
import com.saikou.tvlauncher.receivers.PackageBroadcastReceiver

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppGridAdapter
    private val appList = mutableListOf<AppInfo>()
    private lateinit var prefs: LauncherPreferences
    private lateinit var pkgReceiver: PackageBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = LauncherPreferences(this)
        val count = prefs.getLauncherState(LauncherPreferences.KEY_LAUNCHER_OPENED_COUNT, 0) + 1
        prefs.saveLauncherState(LauncherPreferences.KEY_LAUNCHER_OPENED_COUNT, count)
        prefs.saveLauncherState(
            LauncherPreferences.KEY_LAST_OPENED_TIME,
            System.currentTimeMillis()
        )

        loadApps()
        setupGrid()
        restoreScrollPosition()
        setupBackPress()
        setupPackageReceiver()
        handleHomeIntent(intent)
        checkAndPromptForDefaultLauncher()
    }
    private fun checkAndPromptForDefaultLauncher() {
        if (prefs.getLauncherState(LauncherPreferences.KEY_IS_FIRST_LAUNCH, true)) {
            prefs.saveLauncherState(LauncherPreferences.KEY_IS_FIRST_LAUNCH, false)

            binding.root.postDelayed({
                showDefaultLauncherDialog()
            }, 3000)
        }
    }
    private fun showDefaultLauncherDialog() {
        AlertDialog.Builder(this)
            .setTitle("Launcher o'rnatish")
            .setMessage("Bu ilovani doimiy Home ekran qilmoqchimisiz?\n\n" +
                    "Home tugmasi bosilganda faqat ushbu ilova ochiladi.")
            .setPositiveButton("Ha, o'rnatish") { _, _ ->
                openDefaultLauncherSettings()
            }
            .setNegativeButton("Yo'q", null)
            .setCancelable(false)
            .show()
    }
    private fun openDefaultLauncherSettings() {
        try {
            startActivity(Intent("android.settings.HOME_SETTINGS"))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Sozlamalarni qo‘lda oching: Ilovalar → Default ilovalar → Home", Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { setIntent(it) }
        handleHomeIntent(intent)
    }

    private fun handleHomeIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            moveTaskToBack(false)

            restoreScrollPosition()

            binding.appGrid.requestFocus()
        }
    }

    private fun loadApps() {
        appList.clear()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0).forEach { info ->
            val pkg = info.activityInfo.packageName
            if (pkg != packageName) {
                appList.add(
                    AppInfo(
                        info.loadLabel(pm).toString(),
                        pkg,
                        info.loadIcon(pm)
                    )
                )
            }
        }
        appList.sortBy { it.appName }
    }

    private fun setupGrid() {
        adapter = AppGridAdapter(ArrayList(appList))
        binding.appGrid.apply {
            adapter = this@MainActivity.adapter
            setNumColumns(5)
        }
        adapter.setOnItemClickedListener { app, _ -> launchApp(app.packageName) }
    }

    private fun launchApp(pkg: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            }

            val leanback = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                setPackage(pkg)
            }
            val activity = packageManager.queryIntentActivities(leanback, 0).firstOrNull()
                ?: throw Exception("No launchable activity")

            startActivity(Intent(Intent.ACTION_MAIN).apply {
                setClassName(pkg, activity.activityInfo.name)
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

        } catch (e: Exception) {
            Log.e("TvLauncher", "Launch failed: $pkg", e)
            Toast.makeText(this, "Can't open $pkg", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPackageReceiver() {
        pkgReceiver = PackageBroadcastReceiver { refreshApps() }
        PackageBroadcastReceiver.registerReceiver(this, pkgReceiver)
    }

    private fun refreshApps() {
        loadApps()
        adapter.updateAppItems(appList)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    override fun onPause() {
        super.onPause()
        val pos = binding.appGrid.selectedPosition
        if (pos >= 0) prefs.saveLauncherState(LauncherPreferences.KEY_LAST_SCROLL_POSITION, pos)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(pkgReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun restoreScrollPosition() {
        val pos = prefs.getLauncherState(LauncherPreferences.KEY_LAST_SCROLL_POSITION, 0)
        if (pos in appList.indices) {
            binding.appGrid.post { binding.appGrid.setSelectedPosition(pos) }
        }
    }
}