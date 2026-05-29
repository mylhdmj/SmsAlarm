package com.example.smsalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.util.Calendar

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val sharedPrefs = context.getSharedPreferences("SmsAlarmConfig", Context.MODE_PRIVATE)
            val targetKeyword = sharedPrefs.getString("keyword", "") ?: ""

            if (targetKeyword.isEmpty()) return

            // 1. 周期时间校验 (星期过滤)
            val calendar = Calendar.getInstance()
            // Calendar.DAY_OF_WEEK 返回值: 周日=1, 周一=2 ... 周六=7
            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            // 映射到我们的偏好设置索引 (0=周一, 1=周二 ... 6=周日)
            val prefIndex = when (currentDayOfWeek) {
                Calendar.SUNDAY -> 6
                else -> currentDayOfWeek - 2
            }

            val isDayEnabled = sharedPrefs.getBoolean("day_$prefIndex", true)
            if (!isDayEnabled) return // 如果今天未勾选生效，直接拦截，不提醒

            // 2. 短信文本内容匹配
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody
                if (body.contains(targetKeyword)) {
                    // 满足所有条件，跨进程通知前台服务引报警报
                    val alarmIntent = Intent(context, AlarmService::class.java).apply {
                        action = "TRIGGER_ALARM"
                    }
                    context.startService(alarmIntent)
                    break
                }
            }
        }
    }
}