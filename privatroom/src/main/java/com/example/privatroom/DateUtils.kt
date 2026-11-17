package com.example.privatroom

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private fun formatter() = SimpleDateFormat("dd.MM.yyyy", Locale.US).apply { isLenient = false }

    fun today(): String = formatter().format(Date())

    fun yesterday(): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        return formatter().format(calendar.time)
    }

    fun parse(text: String): Date? {
        if (!Regex("""\d{2}\.\d{2}\.\d{4}""").matches(text)) return null
        return try {
            formatter().parse(text)
        } catch (e: ParseException) {
            null
        }
    }

    fun validate(text: String): String? {
        val date = parse(text) ?: return "Введіть існуючу дату у форматі дд.мм.рррр"
        if (date.after(Date())) return "Не можна обирати майбутню дату"
        val fourYearsAgo = Calendar.getInstance().apply { add(Calendar.YEAR, -4) }.time
        if (date.before(fourYearsAgo)) return "Архів доступний лише за останні 4 роки"
        return null
    }
}