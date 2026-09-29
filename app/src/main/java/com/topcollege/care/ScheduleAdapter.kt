package com.topcollege.care

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(private var lessons: List<LessonModel>) :
    RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLessonNum: TextView = view.findViewById(R.id.tvLessonNum)
        val tvLessonTime: TextView = view.findViewById(R.id.tvLessonTime)
        val tvLessonRoom: TextView = view.findViewById(R.id.tvLessonRoom)
        val tvLessonSubject: TextView = view.findViewById(R.id.tvLessonSubject)
        val tvLessonTeacher: TextView = view.findViewById(R.id.tvLessonTeacher)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lessons[position]
        holder.tvLessonNum.text = item.lesson.toString()
        holder.tvLessonTime.text = "${item.startedAt} - ${item.finishedAt}"
        holder.tvLessonSubject.text = item.subjectName

        if (item.roomName.isNotEmpty()) {
            holder.tvLessonRoom.visibility = View.VISIBLE
            holder.tvLessonRoom.text = "📍 ${item.roomName}"
        } else {
            holder.tvLessonRoom.visibility = View.GONE
        }

        if (item.teacherName.isNotEmpty()) {
            holder.tvLessonTeacher.visibility = View.VISIBLE
            holder.tvLessonTeacher.text = "👤 ${item.teacherName}"
        } else {
            holder.tvLessonTeacher.visibility = View.GONE
        }
    }

    override fun getItemCount() = lessons.size

    fun updateData(newLessons: List<LessonModel>) {
        lessons = newLessons
        notifyDataSetChanged()
    }
}
