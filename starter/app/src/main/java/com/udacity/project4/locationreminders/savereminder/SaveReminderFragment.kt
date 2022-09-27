package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import android.provider.Settings
import android.net.Uri
import com.udacity.project4.BuildConfig


const val GEOFENCE_RADIUS_IN_METERS = 200f
private const val TAG = "SaveReminderFragment"
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 66
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 67
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 68
private const val ACTION_GEOFENCE_EVENT = "SaveReminderFragment.action.ACTION_GEOFENCE_EVENT"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel by sharedViewModel<SaveReminderViewModel>()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private lateinit var appContext: Context
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var newReminder: ReminderDataItem
    private var resultCode = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i("SaveReminderFrag", "inside onCreateView")
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(appContext)

        Log.i("SaveReminderFrag", "before exiting onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            Log.i("SaveReminderFrag", "To Map for selecting location")
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.toSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value ?: ""
            val location = _viewModel.reminderSelectedLocationStr.value ?: ""
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            newReminder = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude
            )
            Log.i("SaveReminderFrag", "Reminder Data is prepared, before starting geofencing")
            checkPermissionsAndStartGeofencing()
        }
    }

    private fun checkPermissionsAndStartGeofencing() {
        Log.i("SaveReminderFrag", "inside CheckPermissionsAndStartGeofencing")
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            Log.i("SaveReminderFrag", "Foreground and background permission approved")
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            Log.i("SaveReminderFrag", "foreground and background permission NOT approved")
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(appContext)
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    Log.i("SaveReminderFrag", "ResolvableAPIException")
                    activity?.let {
                        exception.startResolutionForResult(
                            it,
                            REQUEST_TURN_DEVICE_LOCATION_ON)
                    }
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Log.i("SaveReminderFrag", "not resolvableAPIexception, show snackbar")
                Snackbar.make(
                    this.requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                Log.i("SaveReminderFrag", "Geofence can be added")
                addNewGeofence()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            Log.i("SaveReminderFrag", "requestCode == REQUEST_TURN_DEVICE_LOCATION_ON")
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    @TargetApi(31)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        Log.i("SaveReminderFrag", "Inside requestForegroundAndBackgroundLocationPermissions")
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.i("SaveReminderFrag", "Request foreground only location permission")
        Log.i("SaveReminderFrag","permissionArray:[${permissionsArray}]")
        requestPermissions(
            permissionsArray,
            resultCode
        )

    }
    @TargetApi(31)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            appContext,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                )
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            appContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /**
     * Rewriting the onRequestPermissionsResult
     */
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == resultCode && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            checkDeviceLocationSettingsAndStartGeofence()
//        } else {
//            Snackbar.make(
//                binding.saveReminder,
//                R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
//            ).setAction(android.R.string.ok) {
//                requestForegroundAndBackgroundLocationPermissions()
//            }.show()
//        }
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i("SaveRemindersFrag", "inside onRequestPermissionsResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            Log.i("SaveReminderFrag", "Access is denied and snackbar will be thrown" +
                    "Location services must be enabled to use the app")
            Snackbar.make(
                binding.saveReminder,
                R.string.location_required_error,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            Log.i("SaveReminderFrag", "Permission is not denied - check location setting " +
                    "and start Geofence")
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addNewGeofence() {
        Log.i("SaveReminderFrag", "inside addNewGeofence")
        if (_viewModel.validateAndSaveReminder(newReminder)) {
            val geofence = Geofence.Builder()
                .setRequestId(newReminder.id)
                .setCircularRegion(newReminder.latitude!!, newReminder.longitude!!, GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.e("Add Geofence", geofence.requestId)
                }
                addOnFailureListener {
                    // Failed to add geofences.
                    Toast.makeText(
                        appContext, R.string.geofences_not_added,
                        Toast.LENGTH_SHORT
                    ).show()
                    if ((it.message != null)) {
                        Log.w(TAG, it.message.toString())
                    }
                }
            }
            _viewModel.onClear()
        }
    }


    override fun onDestroy() {
        Log.i("SaveReminderFrag", "inside onDestroy")
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
//        PendingIntent.getBroadcast(contxt, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        PendingIntent.getBroadcast(appContext, 0, intent, PendingIntent.FLAG_MUTABLE)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appContext = context
    }

    override fun onResume() {
        super.onResume()
        appContext = requireContext()
    }

}
