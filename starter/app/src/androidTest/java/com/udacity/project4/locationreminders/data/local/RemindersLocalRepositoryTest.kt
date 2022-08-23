package com.udacity.project4.locationreminders.data.local

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue

import org.hamcrest.MatcherAssert.assertThat

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @Before
    fun setupRepository() {
        application = ApplicationProvider.getApplicationContext()
        remindersDatabase = Room.inMemoryDatabaseBuilder(application, RemindersDatabase::class.java)
            .allowMainThreadQueries().build()

        remindersLocalRepository = RemindersLocalRepository(remindersDatabase.reminderDao(), Dispatchers.Main)
    }
//    TODO: Add testing implementation to the RemindersLocalRepository.kt

    @Test
    fun validateSaveReminders() = runBlocking {
        //GIVEN
        val reminder1 = ReminderDTO("Title1", "reminder title", "Location",
            39.0968, 120.0324)
        remindersLocalRepository.saveReminder(reminder1)

        //WHEN
        val retrieveReminder1: Result.Success<ReminderDTO> =
                remindersLocalRepository.getReminder(reminder1.id) as Result.Success

        //THEN
        assertThat(retrieveReminder1.data, `is`(reminder1))
    }

    @Test
    fun validateDeleteAllReminders_deletesAllReminders() = runBlocking {
        //GIVEN
        val reminder1 = ReminderDTO("Title1", "reminder title1", "Location1",
            39.0968, 120.0324)
        val reminder2 = ReminderDTO("Title2", "reminder title2", "Location2",
            39.0968, 120.0324)
        val reminder3 = ReminderDTO("Title3", "reminder title3", "Location3",
            39.0968, 120.0324)
        remindersLocalRepository.saveReminder(reminder1)
        remindersLocalRepository.saveReminder(reminder2)
        remindersLocalRepository.saveReminder(reminder3)

        //WHEN
        remindersLocalRepository.deleteAllReminders()
        val result1 = remindersLocalRepository.getReminder(reminder1.id) as Result.Error
        val result2 = remindersLocalRepository.getReminder(reminder2.id) as Result.Error
        val result3 = remindersLocalRepository.getReminder(reminder3.id) as Result.Error

        //THEN
        assertThat(result1.message, `is`("Reminder not found!"))
        assertThat(result2.message, `is`("Reminder not found!"))
        assertThat(result3.message, `is`("Reminder not found!"))

    }
}