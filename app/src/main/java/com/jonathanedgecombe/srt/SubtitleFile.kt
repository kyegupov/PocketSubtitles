package com.jonathanedgecombe.srt

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Scanner

class SubtitleFile {
    val subtitles: MutableList<Subtitle>

    /* Create a new SubtitleFile. */
    constructor() {
        subtitles = ArrayList<Subtitle>()
    }

    /* Load an existing SubtitleFile from an InputStream. */
    @Throws(IOException::class, InvalidTimestampFormatException::class)
    constructor(inputStream: InputStream) {
        subtitles = ArrayList<Subtitle>()

        val scanner = Scanner(inputStream)

        while (scanner.hasNextLine()) {
            /* We assign our own ID's, ignore the ID given in the file. */
            do {
                val idOrEmpty = scanner.nextLine()
            } while (idOrEmpty.trim().isEmpty())

            /* Read the Timestamps from the file. */
            val timestamps = scanner.nextLine().split(" --> ".toRegex())
            if (timestamps.size != 2) throw InvalidTimestampFormatException()

            val startTime = Timestamp(timestamps[0])
            val endTime = Timestamp(timestamps[1])

            val subtitle = Subtitle(startTime, endTime, mutableListOf())

            var line = scanner.nextLine()
            while (line != "") {
                subtitle.addLine(line)
                if (!scanner.hasNextLine()) {
                    break
                }
                line = scanner.nextLine()
            }

            subtitles.add(subtitle)
        }
    }

    fun addSubtitle(subtitle: Subtitle) {
        subtitles.add(subtitle)
    }

    fun clearSubtitles() {
        subtitles.clear()
    }

    fun removeSubtitle(subtitle: Subtitle) {
        subtitles.remove(subtitle)
    }

    fun removeSubtitle(index: Int) {
        subtitles.removeAt(index)
    }

    fun getSubtitle(index: Int): Subtitle {
        return subtitles[index]
    }

    fun compile(): String {
        var string = ""

        /* Subtitle indexes start at 1 */
        var index = 1

        for (subtitle in subtitles) {
            string += subtitle.compile(index)
            index++
        }

        return string
    }

    @Throws(IOException::class)
    fun save(file: File) {
        val out = FileOutputStream(file)
        out.write(compile().toByteArray(Charset.forName("UTF-8")))
        out.close()
    }
}
