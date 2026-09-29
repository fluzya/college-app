package com.topcollege.care

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TopAcademyApi(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("top_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val BASE_URL = "https://msapi.top-academy.ru/api/v2"
        private const val APP_KEY = "6a56a5df2667e65aab73ce76d1dd737f7d1faef9c52e8b8c55ac75f565d8e8a6"
    }

    fun saveCredentials(username: String, password: String) {
        prefs.edit()
            .putString("username", username)
            .putString("password", password)
            .remove("token")
            .apply()
    }

    fun getSavedUsername(): String? = prefs.getString("username", null)
    fun getSavedPassword(): String? = prefs.getString("password", null)
    private fun getSavedToken(): String? = prefs.getString("token", null)

    private fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    suspend fun login(user: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("application_key", APP_KEY)
                addProperty("username", user)
                addProperty("password", pass)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/auth/login")
                .post(body)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Origin", "https://journal.top-academy.ru")
                .addHeader("Referer", "https://journal.top-academy.ru/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: return@withContext false
                    val jsonObj = gson.fromJson(respBody, JsonObject::class.java)
                    val token = if (jsonObj.has("accessToken")) {
                        jsonObj.get("accessToken").asString
                    } else if (jsonObj.has("access_token")) {
                        jsonObj.get("access_token").asString
                    } else null

                    if (token != null) {
                        saveCredentials(user, pass)
                        saveToken(token)
                        return@withContext true
                    }
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchSchedule(targetDate: Calendar): Pair<List<LessonModel>, String> = withContext(Dispatchers.IO) {
        val dateStr = String.format(
            "%04d-%02d-%02d",
            targetDate.get(Calendar.YEAR),
            targetDate.get(Calendar.MONTH) + 1,
            targetDate.get(Calendar.DAY_OF_MONTH)
        )

        var token = getSavedToken()
        val user = getSavedUsername()
        val pass = getSavedPassword()

        if (token == null && user != null && pass != null) {
            login(user, pass)
            token = getSavedToken()
        }

        if (token != null) {
            val lessons = requestSchedule(token, dateStr)
            if (lessons != null) {
                return@withContext Pair(lessons, "Журнал ТОП Академии")
            }

            // Токен мог просрочиться, пробуем обновить логин
            if (user != null && pass != null) {
                if (login(user, pass)) {
                    val retryToken = getSavedToken()
                    if (retryToken != null) {
                        val retryLessons = requestSchedule(retryToken, dateStr)
                        if (retryLessons != null) {
                            return@withContext Pair(retryLessons, "Журнал ТОП Академии")
                        }
                    }
                }
            }
        }

        // Резервный шаблон
        return@withContext Pair(getFallbackSchedule(targetDate), "Локальный шаблон (оффлайн)")
    }

    private fun requestSchedule(token: String, dateStr: String): List<LessonModel>? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/schedule/operations/get-by-date?date_filter=$dateStr")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Origin", "https://journal.top-academy.ru")
                .addHeader("Referer", "https://journal.top-academy.ru/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: return null
                    val jsonArray = gson.fromJson(respBody, JsonArray::class.java)
                    val result = mutableListOf<LessonModel>()

                    for (elem in jsonArray) {
                        val obj = elem.asJsonObject
                        val lessonNum = when {
                            obj.has("lesson") && !obj.get("lesson").isJsonNull -> obj.get("lesson").asInt
                            else -> result.size + 1
                        }

                        var started = getStringField(obj, "started_at", "startedAt")
                        var finished = getStringField(obj, "finished_at", "finishedAt")
                        if (started.length > 5) started = started.substring(0, 5)
                        if (finished.length > 5) finished = finished.substring(0, 5)

                        val subject = getStringField(obj, "subject_name", "subjectName", default = "Предмет")
                        val teacher = getStringField(obj, "teacher_name", "teacherName")
                        val room = getStringField(obj, "room_name", "roomName")

                        result.add(
                            LessonModel(
                                lesson = lessonNum,
                                startedAt = started,
                                finishedAt = finished,
                                subjectName = subject,
                                teacherName = teacher,
                                roomName = room
                            )
                        )
                    }
                    result
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getStringField(obj: JsonObject, vararg keys: String, default: String = ""): String {
        for (k in keys) {
            if (obj.has(k) && !obj.get(k).isJsonNull) {
                return obj.get(k).asString
            }
        }
        return default
    }

    private fun getFallbackSchedule(calendar: Calendar): List<LessonModel> {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> listOf(
                LessonModel(1, "09:00", "10:20", "Веб-разработка", "Преподаватель ТОП", "Ауд. 204"),
                LessonModel(2, "10:30", "11:50", "Базы данных", "Преподаватель ТОП", "Ауд. 204")
            )
            Calendar.TUESDAY -> listOf(
                LessonModel(1, "09:00", "10:20", "Программирование Python", "Преподаватель ТОП", "Ауд. 310"),
                LessonModel(2, "10:30", "11:50", "Компьютерные сети", "Преподаватель ТОП", "Ауд. 310")
            )
            Calendar.WEDNESDAY -> listOf(
                LessonModel(1, "09:00", "10:20", "Операционные системы", "Преподаватель ТОП", "Ауд. 105"),
                LessonModel(2, "10:30", "11:50", "Алгоритмы и структуры данных", "Преподаватель ТОП", "Ауд. 105")
            )
            Calendar.THURSDAY -> listOf(
                LessonModel(1, "09:00", "10:20", "Информационная безопасность", "Преподаватель ТОП", "Ауд. 201"),
                LessonModel(2, "10:30", "11:50", "Архитектура ПК", "Преподаватель ТОП", "Ауд. 201")
            )
            Calendar.FRIDAY -> listOf(
                LessonModel(1, "09:00", "10:20", "Проектный практикум", "Преподаватель ТОП", "Ауд. 308"),
                LessonModel(2, "10:30", "11:50", "Тестирование ПО", "Преподаватель ТОП", "Ауд. 308")
            )
            else -> emptyList()
        }
    }
}
