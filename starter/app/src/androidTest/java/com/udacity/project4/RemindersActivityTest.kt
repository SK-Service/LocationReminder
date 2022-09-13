package com.udacity.project4

import android.app.Activity
import org.koin.test.KoinTest
import android.app.Application
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragmentDirections
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@LargeTest
class RemindersActivityTest : KoinTest {


    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    //get hold of the activity context
    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>) : Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }

    @Before
    fun init() {
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        // declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        // Get our real repository
        //repository = get()
        val repository: ReminderDataSource by inject()

        // clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }
    @Test
    fun addNewReminder_ListUpdatedWithNewReminder() = runBlocking {

        // GIVEN - an empty ReminderList
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        val activity = getActivity(activityScenario)
        val navController = mock(NavController::class.java)

        // WHEN - new Reminder added
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("Title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Description"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.select_map)).perform(longClick())
        onView(withId(R.id.save_button)).perform(click())
       onView(withId(R.id.saveReminder)).perform(click())

        // THEN - ReminderList shows item and toast
        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(activity?.window?.decorView))))
            .check(
                matches(
                    isDisplayed()
                )
            )
        onView(withText("Title")).check(matches(isDisplayed()))

        runBlocking {
            delay(6000)
        }

    }



}
