package com.saikou.tvlauncher.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.saikou.tvlauncher.R
import com.saikou.tvlauncher.databinding.AppItemBinding
import com.saikou.tvlauncher.data.model.AppInfo
import com.saikou.tvlauncher.utils.ThemeManager

class AppGridAdapter(
    private val appList: ArrayList<AppInfo> = arrayListOf()
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    private lateinit var onItemClicked: (AppInfo, Int) -> Unit
    private var currentTheme = "dark"

    fun setOnItemClickedListener(listener: (AppInfo, Int) -> Unit) {
        onItemClicked = listener
    }

    fun setCurrentTheme(theme: String) {
        currentTheme = theme
    }

    override fun getItemCount(): Int = appList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AppItemBinding.inflate(inflater, parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(appList[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAppItems(newAppList: List<AppInfo>) {
        this.appList.clear()
        this.appList.addAll(newAppList)
        notifyDataSetChanged()
    }

    inner class AppViewHolder(private val binding: AppItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(app: AppInfo) {
            binding.apply {
                appIcon.setImageDrawable(app.appIcon)
                appName.text = app.appName

                val theme = ThemeManager.getTheme(currentTheme)
                root.setCardBackgroundColor(theme.cardBackgroundColor)
                root.setStrokeColor(theme.strokeColor)
                appName.setTextColor(theme.textColor)

                root.setOnClickListener {
                    root.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction {
                            root.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                            onItemClicked.invoke(app, absoluteAdapterPosition)
                        }
                        .start()
                }

                root.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        root.elevation = 20f
                        root.animate()
                            .scaleX(1.08f)
                            .scaleY(1.08f)
                            .setDuration(200)
                            .start()
                        root.strokeWidth = 3
                    } else {
                        root.elevation = 12f
                        root.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                        root.strokeWidth = 2
                    }

                    val animation = when {
                        hasFocus -> AnimationUtils.loadAnimation(root.context, R.anim.zoom_in)
                        else -> AnimationUtils.loadAnimation(root.context, R.anim.zoom_out)
                    }
                    root.startAnimation(animation)
                    animation.fillAfter = true
                }
            }
        }
    }
}
