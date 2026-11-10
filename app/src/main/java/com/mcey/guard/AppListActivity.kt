package com.mcey.guard

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Yüklü uygulamaları listeler. Kullanıcı herhangi bir uygulamayı
 * elle engelli listeye ekleyip çıkarabilir.
 */
class AppListActivity : AppCompatActivity() {

    data class AppItem(
        val label: String,
        val pkg: String,
        val icon: Drawable
    )

    private lateinit var listView: ListView
    private lateinit var progress: ProgressBar
    private lateinit var txtEmpty: TextView

    private val items = ArrayList<AppItem>()
    private lateinit var adapter: AppAdapter
    private lateinit var blocked: MutableSet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        listView = findViewById(R.id.listApps)
        progress = findViewById(R.id.progress)
        txtEmpty = findViewById(R.id.txtEmpty)

        blocked = Prefs.getCustomPackages(this)
        adapter = AppAdapter()
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = items[position]
            if (blocked.contains(item.pkg)) {
                blocked.remove(item.pkg)
                Prefs.removeCustomPackage(this, item.pkg)
            } else {
                blocked.add(item.pkg)
                Prefs.addCustomPackage(this, item.pkg)
            }
            adapter.notifyDataSetChanged()
        }

        loadApps()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadApps() {
        progress.visibility = View.VISIBLE
        thread {
            val pm = packageManager
            val loaded = ArrayList<AppItem>()
            try {
                val apps = pm.getInstalledApplications(0)
                for (app in apps) {
                    // Başlatılabilir olmayan / kendi paketimiz hariç tut.
                    if (app.packageName == packageName) continue
                    if (pm.getLaunchIntentForPackage(app.packageName) == null) continue
                    val label = pm.getApplicationLabel(app).toString()
                    val icon = pm.getApplicationIcon(app)
                    loaded.add(AppItem(label, app.packageName, icon))
                }
            } catch (_: Exception) {
            }
            loaded.sortBy { it.label.lowercase(Locale.getDefault()) }

            runOnUiThread {
                progress.visibility = View.GONE
                items.clear()
                items.addAll(loaded)
                if (items.isEmpty()) {
                    txtEmpty.visibility = View.VISIBLE
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@AppListActivity)
                .inflate(R.layout.row_app, parent, false)

            val item = items[position]
            view.findViewById<ImageView>(R.id.imgIcon).setImageDrawable(item.icon)
            view.findViewById<TextView>(R.id.txtName).text = item.label
            view.findViewById<TextView>(R.id.txtPkg).text = item.pkg
            view.findViewById<SwitchMaterial>(R.id.swBlocked).isChecked =
                blocked.contains(item.pkg)
            return view
        }
    }
}
