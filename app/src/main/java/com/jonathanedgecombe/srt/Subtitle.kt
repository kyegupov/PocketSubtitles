package com.jonathanedgecombe.srt

data class Subtitle(var startTime: Timestamp,
                    var endTime: Timestamp,
                    val lines: MutableList<String>) {

    fun clearLines() {
        lines.clear()
    }

    fun addLine(line: String) {
        lines.add(line)
    }

    fun removeLine(line: String) {
        lines.remove(line)
    }

    fun removeLine(index: Int) {
        lines.removeAt(index)
    }

    fun getLine(index: Int): String {
        return lines[index]
    }

    /* Compiles subtitle into a string with the given subtitle index. */
    fun compile(index: Int): String {
        var subtitle = ""

        subtitle += Integer.toString(index) + "\n"
        subtitle += startTime.compile() + " --> " + endTime.compile() + "\n"

        for (line in lines) {
            subtitle += line + "\n"
        }

        subtitle += "\n"
        return subtitle
    }

    companion object {

        fun formatLine(line0: String): String {
            var line = line0
            /* Replace CRLF with LF for neatness. */
            line = line.replace("\r\n", "\n")

            /* Empty line marks the end of a subtitle, replace it with a space.  */
            line = line.replace("\n\n", "\n \n")

            return line
        }
    }
}
