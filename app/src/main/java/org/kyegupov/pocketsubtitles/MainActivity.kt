package org.kyegupov.pocketsubtitles

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.*
import com.jonathanedgecombe.srt.Subtitle
import com.jonathanedgecombe.srt.SubtitleFile
import com.jonathanedgecombe.srt.Timestamp
import java.util.*
import android.app.Activity
import android.content.Context
import android.view.*
import android.content.Intent
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

class ListScrollListener(val activity: MainActivity) : RecyclerView.OnScrollListener() {
    override fun onScrolled(listView: RecyclerView, dx: Int, dy: Int){
        if (activity.toggleFollow!!.isChecked) {
            return
        }
        val watermarkY = activity.watermark!!.top
        val itemView = listView.findChildViewUnder(10f, watermarkY.toFloat())
        if (itemView != null) {
            val itemIndex = listView.getChildAdapterPosition(itemView)
            val sub = activity.subtitles[itemIndex]
            val startSeconds = sub.startTime.totalMillis
            val endSeconds = sub.endTime.totalMillis
            val relativeInItem = (watermarkY - itemView.y) / itemView.measuredHeight
            val time = startSeconds + (endSeconds - startSeconds) * relativeInItem
            activity.timeLabel!!.text = Timestamp.fromTotalMillis(time.toLong()).compile()
        }
    }
}

class MainActivity : AppCompatActivity() {

    private val  FILE_SELECT_CODE = 100

    internal var watermark : View? = null
    internal var subtitles : List<Subtitle> = listOf()
    internal var timeLabel: TextView? = null
    internal var offsetMillis: Long? = null
    internal var toggleFollow: ToggleButton? = null
    private var startingTimestamps: List<Timestamp> = listOf()
    private var listView: RecyclerView? = null

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
                    loadSubs(contentResolver.openInputStream(uri))
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.subs_list) as RecyclerView

        val mLayoutManager = LinearLayoutManager(this)
        listView?.layoutManager = mLayoutManager

        listView?.setHasFixedSize(true)
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

        loadSubs(testSubStream)

        listView?.addOnScrollListener(ListScrollListener(this))

        watermark = findViewById(R.id.watermark) as View

        timeLabel = findViewById(R.id.current_time) as TextView

        var timer = null as Timer?
        val activity = this

        toggleFollow = findViewById(R.id.toggle_follow) as ToggleButton
        toggleFollow!!.setOnClickListener {
            val tb = it as ToggleButton
            if (tb.isChecked) {
                timer = Timer()
                val advanceTime = object : TimerTask() {
                    override fun run() {
                        activity.runOnUiThread {
                            val timestamp = Timestamp.fromTotalMillis(System.currentTimeMillis() - offsetMillis!!)
                            timeLabel!!.text = timestamp.compile()
                            val binSearchIndex = startingTimestamps.binarySearch(timestamp)
                            val subtitleToScrollTo = if (binSearchIndex > 0) {
                                if (binSearchIndex < 1) 1 else binSearchIndex
                            } else {
                                val previousItem = -2 - binSearchIndex
                                if (previousItem < 1) 1 else previousItem
                            }
                            val viewHolder = listView?.findViewHolderForAdapterPosition(subtitleToScrollTo)
                            if (viewHolder == null) {
                                listView?.scrollToPosition(subtitleToScrollTo)
                            } else {
                                val watermarkY = activity.watermark!!.top
                                listView?.scrollBy(0, viewHolder.itemView.top - watermarkY)
                            }

                        }

                    }

                }
                timer!!.schedule(advanceTime, 100 /* ms */, 100 /* ms */)
                offsetMillis = System.currentTimeMillis() - Timestamp(timeLabel!!.text).totalMillis
//                listView.isLayoutFrozen = true
            } else {
                timer!!.cancel()
//                listView.isLayoutFrozen = false
            }
        }
    }

    private fun loadSubs(testSubStream: InputStream) {
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
        subtitles = subsWithPadding

        this.startingTimestamps = subtitles.map {it.startTime}
        this.listView?.adapter = SubtitleListAdapter(subtitles)
    }
}
