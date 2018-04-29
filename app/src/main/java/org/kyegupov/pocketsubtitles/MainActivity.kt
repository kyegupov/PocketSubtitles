package org.kyegupov.pocketsubtitles

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.jonathanedgecombe.srt.Subtitle
import com.jonathanedgecombe.srt.SubtitleFile
import com.jonathanedgecombe.srt.Timestamp
import java.util.*
import android.app.Activity
import android.view.*
import android.content.Intent
import android.widget.TextView
import android.widget.ToggleButton
import java.io.InputStream


class SubtitleListAdapter(val objects: List<Subtitle>)
    : RecyclerView.Adapter<SimpleViewHolder>() {
    override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
        val sub = objects[position]
        (holder.itemView.findViewById(R.id.subtitle_text) as TextView).text =
                sub.lines.joinToString("\n")
        (holder.itemView.findViewById(R.id.subtitle_timestamp) as TextView).text =
                "${sub.startTime.compile()} --> ${sub.endTime.compile()}"
    }

    override fun getItemCount(): Int {
        return objects.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
        return SimpleViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.subs_list_item, parent, false))
    }
}

class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class ListScrollListener(val state: AppState) : RecyclerView.OnScrollListener() {
    override fun onScrolled(listView: RecyclerView, dx: Int, dy: Int){
        if (state.toggleFollow.isChecked) {
            return
        }
        val watermarkY = state.watermark.top
        val itemView = listView.findChildViewUnder(10f, watermarkY.toFloat())
        if (itemView != null) {
            val itemIndex = listView.getChildAdapterPosition(itemView)
            val sub = state.subtitles[itemIndex]
            val startSeconds = sub.startTime.totalMillis
            val endSeconds = sub.endTime.totalMillis
            val relativeInItem = (watermarkY - itemView.y) / itemView.measuredHeight
            val time = startSeconds + (endSeconds - startSeconds) * relativeInItem
            state.timeLabel.text = Timestamp.fromTotalMillis(time.toLong()).compile()
        }
    }
}

data class AppState(
    // Widgets
        var listView: RecyclerView,
        var watermark : View,
        var timeLabel: TextView,
        var toggleFollow: ToggleButton,

    // Data
        var offsetMillis: Long,
        var subtitles : List<Subtitle>,
        var startingTimestamps: List<Timestamp>
)

class MainActivity : AppCompatActivity() {

    private val  FILE_SELECT_CODE = 100
    internal var state: AppState? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun openFile() {
        val myIntent = Intent(Intent.ACTION_GET_CONTENT, null)
        myIntent.type = "*/*"
        startActivityForResult(myIntent, FILE_SELECT_CODE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.open_file -> {
                openFile()
                return true
            }
//            R.id.help -> {
//                showHelp()
//                return true
//            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_SELECT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                    val uri = data.data
                    loadSubs(contentResolver.openInputStream(uri), state!!)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val state = AppState(
            watermark = findViewById(R.id.watermark) as View,
            timeLabel = findViewById(R.id.current_time) as TextView,
            offsetMillis = 0,
            toggleFollow = findViewById(R.id.toggle_follow) as ToggleButton,
            listView = findViewById(R.id.subs_list) as RecyclerView,
            subtitles = listOf(),
            startingTimestamps = listOf()
        )
        this.state = state

        val mLayoutManager = LinearLayoutManager(this)
        state.listView.layoutManager = mLayoutManager

        state.listView.setHasFixedSize(true)
// TODO: enable speed controls
//        val SPEEDS = listOf("24/30","25/30","24/25","1","25/24","30/25","30/24")

//        val speeds = findViewById(R.id.speed) as Spinner
//        val adapter = ArrayAdapter(
//                this,
//                android.R.layout.simple_spinner_item,
//                SPEEDS)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        speeds.adapter = adapter

        val testSubStream = this.resources.openRawResource(R.raw.test)

        loadSubs(testSubStream, state)

        state.listView.addOnScrollListener(ListScrollListener(state))

        var timer = null as Timer?
        val activity = this

        state.toggleFollow.setOnClickListener {
            val tb = it as ToggleButton
            if (tb.isChecked) {
                timer = Timer()
                val advanceTime = object : TimerTask() {
                    override fun run() {
                        activity.runOnUiThread {
                            val timestamp = Timestamp.fromTotalMillis(System.currentTimeMillis() - state.offsetMillis)
                            state.timeLabel.text = timestamp.compile()
                            val binSearchIndex = state.startingTimestamps.binarySearch(timestamp)
                            val subtitleToScrollTo = if (binSearchIndex > 0) {
                                if (binSearchIndex < 1) 1 else binSearchIndex
                            } else {
                                val previousItem = -2 - binSearchIndex
                                if (previousItem < 1) 1 else previousItem
                            }
                            val currentSubtitle = state.subtitles[subtitleToScrollTo]
                            var progress = (1.0 * (timestamp.totalMillis - currentSubtitle.startTime.totalMillis)
                                    / (currentSubtitle.endTime.totalMillis - currentSubtitle.startTime.totalMillis))
                            if (progress > 1) {
                                progress = 1.0
                            }
                            val viewHolder = state.listView.findViewHolderForAdapterPosition(subtitleToScrollTo)
                            if (viewHolder == null) {
                                state.listView.scrollToPosition(subtitleToScrollTo)
                            } else {
                                val watermarkY = state.watermark.top
                                state.listView.scrollBy(0, viewHolder.itemView.top + (viewHolder.itemView.height * progress).toInt() - watermarkY)
                            }

                        }

                    }

                }
                timer!!.schedule(advanceTime, 100 /* ms */, 100 /* ms */)
                state.offsetMillis = System.currentTimeMillis() - Timestamp(state.timeLabel.text).totalMillis
            } else {
                timer!!.cancel()
            }
        }
    }

    private fun loadSubs(testSubStream: InputStream, state: AppState) {
        val subsFile = SubtitleFile(testSubStream)
        val subsWithPadding = mutableListOf<Subtitle>()
        subsWithPadding.add(Subtitle(
                Timestamp.fromTotalMillis(0),
                Timestamp.fromTotalMillis(0),
                Collections.nCopies(30, "")))
        subsWithPadding.addAll(subsFile.subtitles)
        subsWithPadding.add(Subtitle(
                Timestamp.fromTotalMillis(999999999),
                Timestamp.fromTotalMillis(999999999),
                Collections.nCopies(30, "")))
        state.subtitles = subsWithPadding

        state.startingTimestamps = state.subtitles.map {it.startTime}
        state.listView.adapter = SubtitleListAdapter(state.subtitles)
    }
}
