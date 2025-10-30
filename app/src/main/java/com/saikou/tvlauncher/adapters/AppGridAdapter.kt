package com.saikou.tvlauncher.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.saikou.tvlauncher.R
import com.saikou.tvlauncher.databinding.AppItemBinding
import com.saikou.tvlauncher.data.model.AppInfo

class AppGridAdapter(
    private val appList: ArrayList<AppInfo> = arrayListOf()
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    private lateinit var onItemClicked: (AppInfo, Int) -> Unit

    fun setOnItemClickedListener(listener: (AppInfo, Int) -> Unit) {
        onItemClicked = listener
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

                root.setOnClickListener {
                    onItemClicked.invoke(app, absoluteAdapterPosition)
                }

                binding.root.setOnFocusChangeListener { _, hasFocus ->
                    val animation = when {
                        hasFocus -> AnimationUtils.loadAnimation(
                            binding.root.context, R.anim.zoom_in
                        )
                        else -> AnimationUtils.loadAnimation(
                            binding.root.context, R.anim.zoom_out
                        )
                    }
                    binding.root.startAnimation(animation)
                    animation.fillAfter = true
                }
            }
        }
    }
}
