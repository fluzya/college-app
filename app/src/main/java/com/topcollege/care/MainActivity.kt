package com.topcollege.care

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var api: TopAcademyApi
    private var isViewingTomorrow = false

    // Request Notification permission for Android 13+
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                NotificationHelper.scheduleAlarms(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        api = TopAcademyApi(this)

        // Инициализация уведомлений и каналов
        NotificationHelper.createNotificationChannels(this)
        checkNotificationPermission()
        NotificationHelper.scheduleAlarms(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_schedule -> {
                    showScheduleView()
                    true
                }
                R.id.nav_skincare -> {
                    showSkincareView()
                    true
                }
                R.id.nav_settings -> {
                    showSettingsView()
                    true
                }
                else -> false
            }
        }

        // По умолчанию открываем экран расписания
        showScheduleView()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- 1. ЭКРАН РАСПИСАНИЯ ---
    private fun showScheduleView() {
        val container = findViewById<FrameLayout>(R.id.contentContainer)
        container.removeAllViews()
        val view = layoutInflater.inflate(R.layout.fragment_schedule, container, false)
        container.addView(view)

        val btnToday = view.findViewById<MaterialButton>(R.id.btnToday)
        val btnTomorrow = view.findViewById<MaterialButton>(R.id.btnTomorrow)
        val tvSourceStatus = view.findViewById<TextView>(R.id.tvSourceStatus)
        val tvEmptySchedule = view.findViewById<TextView>(R.id.tvEmptySchedule)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val rvSchedule = view.findViewById<RecyclerView>(R.id.rvSchedule)

        rvSchedule.layoutManager = LinearLayoutManager(this)
        val adapter = ScheduleAdapter(emptyList())
        rvSchedule.adapter = adapter

        fun loadData() {
            swipeRefresh.isRefreshing = true
            val cal = Calendar.getInstance()
            if (isViewingTomorrow) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            lifecycleScope.launch {
                val (lessons, source) = api.fetchSchedule(cal)
                swipeRefresh.isRefreshing = false
                tvSourceStatus.text = "Источник: $source"

                if (lessons.isEmpty()) {
                    tvEmptySchedule.visibility = View.VISIBLE
                    rvSchedule.visibility = View.GONE
                } else {
                    tvEmptySchedule.visibility = View.GONE
                    rvSchedule.visibility = View.VISIBLE
                    adapter.updateData(lessons)
                }
            }
        }

        btnToday.setOnClickListener {
            if (isViewingTomorrow) {
                isViewingTomorrow = false
                btnToday.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                btnTomorrow.setBackgroundColor(ContextCompat.getColor(this, R.color.divider))
                loadData()
            }
        }

        btnTomorrow.setOnClickListener {
            if (!isViewingTomorrow) {
                isViewingTomorrow = true
                btnTomorrow.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                btnToday.setBackgroundColor(ContextCompat.getColor(this, R.color.divider))
                loadData()
            }
        }

        swipeRefresh.setOnRefreshListener {
            loadData()
        }

        loadData()
    }

    // --- 2. ЭКРАН УХОДА ЗА ЛИЦОМ ---
    private fun showSkincareView() {
        val container = findViewById<FrameLayout>(R.id.contentContainer)
        container.removeAllViews()
        val view = layoutInflater.inflate(R.layout.fragment_skincare, container, false)
        container.addView(view)

        val tvDayTitle = view.findViewById<TextView>(R.id.tvCareDayTitle)
        val tvDayNote = view.findViewById<TextView>(R.id.tvCareDayNote)
        val layoutMorning = view.findViewById<LinearLayout>(R.id.layoutMorningCheckboxes)
        val layoutEvening = view.findViewById<LinearLayout>(R.id.layoutEveningCheckboxes)

        val today = Calendar.getInstance()
        val dateKey = String.format("%04d%02d%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
        val routine = SkincareManager.getRoutineForDay(today)

        tvDayTitle.text = routine.title
        if (routine.note.isNotEmpty()) {
            tvDayNote.visibility = View.VISIBLE
            tvDayNote.text = "💡 ${routine.note}"
        } else {
            tvDayNote.visibility = View.GONE
        }

        // Заполняем утренние чекбоксы
        layoutMorning.removeAllViews()
        routine.morningSteps.forEachIndexed { idx, step ->
            val cb = CheckBox(this).apply {
                text = step
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                textSize = 14f
                isChecked = SkincareManager.isStepCompleted(this@MainActivity, dateKey, idx, isEvening = false)
                setOnCheckedChangeListener { _, checked ->
                    SkincareManager.setStepCompleted(this@MainActivity, dateKey, idx, isEvening = false, checked)
                }
            }
            layoutMorning.addView(cb)
        }

        // Заполняем вечерние чекбоксы
        layoutEvening.removeAllViews()
        routine.eveningSteps.forEachIndexed { idx, step ->
            val cb = CheckBox(this).apply {
                text = step
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                textSize = 14f
                isChecked = SkincareManager.isStepCompleted(this@MainActivity, dateKey, idx, isEvening = true)
                setOnCheckedChangeListener { _, checked ->
                    SkincareManager.setStepCompleted(this@MainActivity, dateKey, idx, isEvening = true, checked)
                }
            }
            layoutEvening.addView(cb)
        }
    }

    // --- 3. ЭКРАН НАСТРОЕК ---
    private fun showSettingsView() {
        val container = findViewById<FrameLayout>(R.id.contentContainer)
        container.removeAllViews()
        val view = layoutInflater.inflate(R.layout.fragment_settings, container, false)
        container.addView(view)

        val etUsername = view.findViewById<TextInputEditText>(R.id.etTopUsername)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etTopPassword)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveLogin)
        val tvStatus = view.findViewById<TextView>(R.id.tvLoginStatus)
        val btnTestMorning = view.findViewById<MaterialButton>(R.id.btnTestMorningNotif)
        val btnTestEvening = view.findViewById<MaterialButton>(R.id.btnTestEveningNotif)

        val savedUser = api.getSavedUsername()
        if (savedUser != null) {
            etUsername.setText(savedUser)
            tvStatus.text = "Статус: Авторизован как $savedUser ✅"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent))
        }

        btnSave.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Введи логин и пароль!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            tvStatus.text = "Проверка авторизации в ТОП Академии..."
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.gold))

            lifecycleScope.launch {
                val success = api.login(user, pass)
                btnSave.isEnabled = true
                if (success) {
                    tvStatus.text = "Успешно! Расписание привязано к $user ✅"
                    tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent))
                    Toast.makeText(this@MainActivity, "Авторизация успешна!", Toast.LENGTH_SHORT).show()
                } else {
                    tvStatus.text = "Ошибка входа. Проверь логин/пароль ❌"
                    tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_light))
                    Toast.makeText(this@MainActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Мгновенная проверка пуш-уведомлений
        btnTestMorning.setOnClickListener {
            NotificationHelper.showMorningDigest(this)
            Toast.makeText(this, "Утренний пуш 06:00 отправлен!", Toast.LENGTH_SHORT).show()
        }

        btnTestEvening.setOnClickListener {
            NotificationHelper.showEveningSkincare(this)
            Toast.makeText(this, "Вечерний пуш 22:00 отправлен!", Toast.LENGTH_SHORT).show()
        }
    }
}
