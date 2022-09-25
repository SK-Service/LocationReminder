package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        //        receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            Log.i("RemDescription", "Inside companion object - new Intent")
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            Log.i("RemDescription", "ReminderDataItem:${reminderDataItem.description}")
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    private lateinit var binding: ActivityReminderDescriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("RemDescription", "Inside OnCreate")
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )

        Log.i("RemDescription", "Before extracting reminder data")
        Log.i("RemDescription", "extract from intent: " +
                        "<${intent.getSerializableExtra(EXTRA_ReminderDataItem)}>")
        binding.reminderDataItem =
            intent.getSerializableExtra(EXTRA_ReminderDataItem) as ReminderDataItem
        binding.closeButton.setOnClickListener {
            finish()
        }
    }
}
