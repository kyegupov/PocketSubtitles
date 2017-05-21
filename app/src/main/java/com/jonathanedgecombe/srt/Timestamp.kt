package com.jonathanedgecombe.srt

import android.annotation.SuppressLint

class Timestamp : Comparable<Timestamp> {

    var hours: Int = 0
    var minutes: Int = 0
    var seconds: Int = 0
    var milliseconds: Int = 0

    /* Create a new timestamp at the given time. */
    constructor(hours: Int, minutes: Int, seconds: Int, milliseconds: Int) {
        this.hours = hours
        this.minutes = minutes
        this.seconds = seconds
        this.milliseconds = milliseconds
    }

    /* Create a new timestamp from the given string.
	 * Uses the SRT timestamp format:
	 * hours:minutes:seconds,milliseconds
	 * eg. 00:00:28,400 */
    @Throws(InvalidTimestampFormatException::class)
    constructor(time: CharSequence) {
        val topParts = time.split(",".toRegex()).dropLastWhile { it.isEmpty() }
        if (topParts.size != 2) throw InvalidTimestampFormatException()
        val parts = topParts[0].split(":".toRegex()).dropLastWhile { it.isEmpty() }
        if (parts.size != 3) throw InvalidTimestampFormatException()

        this.hours = Integer.parseInt(parts[0])
        this.minutes = Integer.parseInt(parts[1])
        this.seconds = Integer.parseInt(parts[2])
        this.milliseconds = Integer.parseInt(topParts[1])
    }

    /* Compiles the timestamp to an SRT timestamp. */
    @SuppressLint("DefaultLocale") // Not relevant for integers
    fun compile(): String {
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }

    val totalMillis: Long
        get() = milliseconds + 1000 * (seconds + (60 * (minutes + 60 * hours))).toLong()

    companion object {
        fun fromTotalMillis(ms: Long) : Timestamp {
            val s = ms / 1000
            val totalMinutes = (s / 60).toInt()
            return Timestamp(
                    totalMinutes / 60,
                    totalMinutes % 60,
                    (s % 60).toInt(),
                    (ms % 1000).toInt())
        }
    }

    override fun compareTo(other: Timestamp): Int {
        return this.totalMillis.compareTo(other.totalMillis)
    }
}
