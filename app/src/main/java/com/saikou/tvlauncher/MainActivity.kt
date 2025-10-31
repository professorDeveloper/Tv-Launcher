package com.saikou.tvlauncher

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.saikou.tvlauncher.adapters.AppGridAdapter
import com.saikou.tvlauncher.data.local.LauncherPreferences
import com.saikou.tvlauncher.data.model.AppInfo
import com.saikou.tvlauncher.databinding.ActivityMainBinding
import com.saikou.tvlauncher.receivers.PackageBroadcastReceiver
import java.text.SimpleDateFormat
import java.util.*

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

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(600).start()

        startClock()

        val openedCount = prefs.getLauncherState(LauncherPreferences.KEY_LAUNCHER_OPENED_COUNT, 0) + 1
        prefs.saveLauncherState(LauncherPreferences.KEY_LAUNCHER_OPENED_COUNT, openedCount)
        prefs.saveLauncherState(LauncherPreferences.KEY_LAST_OPENED_TIME, System.currentTimeMillis())

        loadApps()
        setupGrid()
        restoreScrollPosition()
        setupBackPress()
        setupPackageReceiver()
        handleHomeIntent(intent)
        setupSearch()
        setupSettingsButton()
        if (prefs.getLauncherState(LauncherPreferences.KEY_IS_FIRST_LAUNCH, true)) {
            prefs.saveLauncherState(LauncherPreferences.KEY_IS_FIRST_LAUNCH, false)
            binding.root.postDelayed({ showDefaultLauncherDialog() }, 3000)

        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val filtered = appList.filter { it.appName.lowercase().contains(query) }

            adapter.updateAppItems(filtered)
            binding.appGrid.scheduleLayoutAnimation()
        }

    }

    private fun setupSettingsButton() {
        binding.settingsButton.setOnClickListener {
            binding.settingsButton.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    binding.settingsButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                }
                .start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isDefaultLauncher()) {
            showDefaultLauncherDialog()
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun startClock() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post(object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                val sdfDate = SimpleDateFormat("d MMM, EEEE", Locale("uz", "UZ"))
                binding.clock.text = sdfTime.format(Date())
                binding.date.text = " | ${sdfDate.format(Date())}"
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun showDefaultLauncherDialog() {
        AlertDialog.Builder(this)
            .setTitle("Launcher o'rnatish")
            .setMessage("Bu ilovani doimiy Home ekran qilmoqchimisiz?\n\nHome tugmasi bosilganda faqat ushbu ilova ochiladi.")
            .setPositiveButton("Ha, o'rnatish") { _, _ -> openDefaultLauncherSettings() }
            .setNegativeButton("Yo'q", null)
            .setCancelable(false)
            .show()
    }

    private fun openDefaultLauncherSettings() {
        var intent = Intent("com.android.tv.action.HOME_SETTINGS").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (canResolveIntent(intent)) { startActivity(intent); return }

        intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (canResolveIntent(intent)) { startActivity(intent); return }

    }

    private fun canResolveIntent(intent: Intent): Boolean = packageManager.resolveActivity(intent, 0) != null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { setIntent(it) }
        handleHomeIntent(intent)
    }

    private fun handleHomeIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            binding.root.requestFocus()
        }
    }

    private fun loadApps() {
        appList.clear()
        val pm = packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in installedApps) {
            val pkg = app.packageName
            if (pkg == packageName) continue

            var intent: Intent? = pm.getLaunchIntentForPackage(pkg)

            if (intent == null) {
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(pkg)
                }
                val resolve = pm.resolveActivity(launcherIntent, 0)
                if (resolve != null) {
                    intent = Intent(Intent.ACTION_MAIN).apply {
                        setClassName(pkg, resolve.activityInfo.name)
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                }
            }

            if (intent == null) {
                val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    setPackage(pkg)
                }
                val resolve = pm.resolveActivity(leanbackIntent, 0)
                if (resolve != null) {
                    intent = Intent(Intent.ACTION_MAIN).apply {
                        setClassName(pkg, resolve.activityInfo.name)
                        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    }
                }
            }

            if (intent != null) {
                val label = app.loadLabel(pm).toString()
                val icon = app.loadIcon(pm)
                appList.add(AppInfo(label, pkg, icon))
            }
        }

        appList.sortBy { it.appName.lowercase() }
    }

    private fun setupGrid() {
        adapter = AppGridAdapter(ArrayList(appList))
        binding.appGrid.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 6)
            setHasFixedSize(true)
            adapter = this@MainActivity.adapter
        }
        adapter.setOnItemClickedListener { app, _ -> launchApp(app.packageName) }
    }

    private fun launchApp(pkg: String) {
        try {
            var intent = packageManager.getLaunchIntentForPackage(pkg)

            if (intent == null) {
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(pkg)
                }
                val resolve = packageManager.resolveActivity(launcherIntent, 0)
                if (resolve != null) {
                    intent = Intent(Intent.ACTION_MAIN).apply {
                        setClassName(pkg, resolve.activityInfo.name)
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                }
            }

            if (intent == null) {
                val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    setPackage(pkg)
                }
                val resolve = packageManager.resolveActivity(leanbackIntent, 0)
                if (resolve != null) {
                    intent = Intent(Intent.ACTION_MAIN).apply {
                        setClassName(pkg, resolve.activityInfo.name)
                        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    }
                }
            }

            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent ?: throw Exception("No intent"))
        } catch (e: Exception) {
            Log.e("TvLauncher", "Launch failed: $pkg", e)
            Toast.makeText(this, "Ilovani ochib bo'lmadi: $pkg", Toast.LENGTH_SHORT).show()
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
            override fun handleOnBackPressed() { }
        })
    }

    override fun onPause() {
        super.onPause()
        val pos = (binding.appGrid.layoutManager as? GridLayoutManager)
            ?.findFirstCompletelyVisibleItemPosition() ?: -1
        if (pos >= 0) prefs.saveLauncherState(LauncherPreferences.KEY_LAST_SCROLL_POSITION, pos)
    }

    override fun onDestroy() {
        try { unregisterReceiver(pkgReceiver) } catch (_: Exception) { }
        super.onDestroy()
    }

    private fun restoreScrollPosition() {
        val pos = prefs.getLauncherState(LauncherPreferences.KEY_LAST_SCROLL_POSITION, 0)
        if (pos in appList.indices) {
            binding.appGrid.post {
                binding.appGrid.scrollToPosition(pos)
                (binding.appGrid.layoutManager as GridLayoutManager)
                    .findViewByPosition(pos)?.requestFocus()
            }
        }
    }
}
