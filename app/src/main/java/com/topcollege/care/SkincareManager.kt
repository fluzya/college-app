package com.topcollege.care

import android.content.Context
import java.util.Calendar

object SkincareManager {

    private val baseMorning = listOf(
        "1. Умывание (мягкая пенка + теплая вода)",
        "2. Тоник Pechoin (нанести руками на лицо)",
        "3. Крем Aravia с витамином С (тонким слоем)",
        "4. SPF-крем (через 5 минут после крема)"
    )

    private val baseEvening = listOf(
        "1. Умывание (базовая мягкая пенка + вода)",
        "2. Средство от акне YOU&PURE (точечно строго на прыщи)",
        "3. Через 15 минут: Восстанавливающий синий крем Pechoin на все лицо"
    )

    private val deepCleansingEvening = listOf(
        "1. Умывание базовой пенкой",
        "2. Энзимная пудра Aravia (размылить в ладони до пены без крупинок, массаж 1 мин, смыть)",
        "3. Тоник Pechoin руками на лицо",
        "4. Синий крем Pechoin плотным слоем",
        "⚠️ Точечное средство от акне сегодня пропустить!"
    )

    private val barrierRecoveryMorning = listOf(
        "1. Умывание базовой пенкой",
        "2. Тоник Pechoin руками",
        "3. Синий крем Pechoin легким слоем (кожа отдыхает от витамина С)",
        "4. SPF-крем"
    )

    private val barrierRecoveryEvening = listOf(
        "1. Умывание базовой пенкой",
        "2. Только синий крем Pechoin плотным слоем на все лицо (восстановление без активов)"
    )

    fun getRoutineForDay(calendar: Calendar): SkincareDay {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> SkincareDay(
                title = "ПОНЕДЕЛЬНИК",
                morningSteps = baseMorning,
                eveningSteps = baseEvening
            )
            Calendar.TUESDAY -> SkincareDay(
                title = "ВТОРНИК (Глубокое очищение)",
                morningSteps = baseMorning,
                eveningSteps = deepCleansingEvening,
                note = "Вечером: Энзимная пудра вместо средства от акне"
            )
            Calendar.WEDNESDAY -> SkincareDay(
                title = "СРЕДА",
                morningSteps = baseMorning,
                eveningSteps = baseEvening
            )
            Calendar.THURSDAY -> SkincareDay(
                title = "ЧЕТВЕРГ",
                morningSteps = baseMorning,
                eveningSteps = baseEvening
            )
            Calendar.FRIDAY -> SkincareDay(
                title = "ПЯТНИЦА (Глубокое очищение)",
                morningSteps = baseMorning,
                eveningSteps = deepCleansingEvening,
                note = "Вечером: Энзимная пудра вместо средства от акне"
            )
            Calendar.SATURDAY -> SkincareDay(
                title = "СУББОТА",
                morningSteps = baseMorning,
                eveningSteps = baseEvening
            )
            Calendar.SUNDAY -> SkincareDay(
                title = "ВОСКРЕСЕНЬЕ (Заживление барьера)",
                morningSteps = barrierRecoveryMorning,
                eveningSteps = barrierRecoveryEvening,
                note = "Кожа отдыхает: без витамина С и без активов"
            )
            else -> SkincareDay("ПОНЕДЕЛЬНИК", baseMorning, baseEvening)
        }
    }

    fun isStepCompleted(context: Context, dateKey: String, stepIndex: Int, isEvening: Boolean): Boolean {
        val prefs = context.getSharedPreferences("skincare_progress", Context.MODE_PRIVATE)
        val prefix = if (isEvening) "eve" else "morn"
        return prefs.getBoolean("${dateKey}_${prefix}_$stepIndex", false)
    }

    fun setStepCompleted(context: Context, dateKey: String, stepIndex: Int, isEvening: Boolean, completed: Boolean) {
        val prefs = context.getSharedPreferences("skincare_progress", Context.MODE_PRIVATE)
        val prefix = if (isEvening) "eve" else "morn"
        prefs.edit().putBoolean("${dateKey}_${prefix}_$stepIndex", completed).apply()
    }
}
