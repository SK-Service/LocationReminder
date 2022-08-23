package com.udacity.project4.locationreminders.data.local

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

//    TODO: Add testing implementation to the RemindersDao.kt
@get:Rule
var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var remindersDatabase: RemindersDatabase

    @Before
    fun setupDatabase() {
        application = ApplicationProvider.getApplicationContext()
        remindersDatabase = Room.inMemoryDatabaseBuilder(application, RemindersDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @Test
    fun validateRetrieveReminder () = runBlocking {
        //GIVEN
        val reminder1 = ReminderDTO("Title1", "reminder title", "Location",
        39.0968, 120.0324)

        remindersDatabase.reminderDao().saveReminder(reminder1)

        //WHEN
        val retrieveReminder1: ReminderDTO? = remindersDatabase.reminderDao().
                        getReminderById(reminder1.id )

       // THEN
        assertThat(retrieveReminder1, `is` (reminder1))
    }

    @Test
    fun deleteAllReminders_noRemindersRemaining () = runBlocking {

        //GIVEN
        val reminder1 = ReminderDTO("Title1", "reminder title1", "Location1",
            39.0968, 120.0324)

        val reminder2 = ReminderDTO("Title2", "reminder title2", "Location2",
            39.0968, 120.0324)

        val reminder3 = ReminderDTO("Title3", "reminder title3", "Location3",
            39.0968, 120.0324)

        remindersDatabase.reminderDao().saveReminder(reminder1)
        remindersDatabase.reminderDao().saveReminder(reminder2)
        remindersDatabase.reminderDao().saveReminder(reminder3)

        //WHEN

        remindersDatabase.reminderDao().deleteAllReminders()
        val retrieveReminder1: ReminderDTO? = remindersDatabase.reminderDao().
        getReminderById(reminder1.id )
        val retrieveReminder2: ReminderDTO? = remindersDatabase.reminderDao().
        getReminderById(reminder2.id )
        val retrieveReminder3: ReminderDTO? = remindersDatabase.reminderDao().
        getReminderById(reminder3.id )

        //THEN
        assertThat(retrieveReminder1, `is` (nullValue()))
        assertThat(retrieveReminder2, `is` (nullValue()))
        assertThat(retrieveReminder3, `is` (nullValue()))
    }

}