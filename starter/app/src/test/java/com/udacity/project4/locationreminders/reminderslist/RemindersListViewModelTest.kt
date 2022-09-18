package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var dataSource: FakeDataSource
    private lateinit var application: Application

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        stopKoin()
        application = ApplicationProvider.getApplicationContext()
        FirebaseApp.initializeApp(application)
        dataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(application, dataSource)
    }

    @Test
    fun check_loading() = runTest(UnconfinedTestDispatcher()) {
        val reminder = ReminderDTO("Title", "Description", "Location", 19.0, 20.2)


        //mainCoroutineRule.pauseDispatcher()
        val job = launch {
            dataSource.saveReminder(reminder)
        }
        job.cancel()
        remindersListViewModel.loadReminders()
        assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true)
        )
        job.start()
        //  mainCoroutineRule.resumeDispatcher()
        assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false)
        )

    }

    @Test
    fun shouldReturnError() = runTest(UnconfinedTestDispatcher()) {
        dataSource.setReturnsError(true)
        remindersListViewModel.loadReminders()

        assertThat(
            remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`(notNullValue())
        )
    }

}