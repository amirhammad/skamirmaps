
package sk.amir.maps

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.permissions.PermissionsListener
import org.maplibre.android.location.permissions.PermissionsManager
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import sk.amir.maps.helpers.getActivity
import java.util.Arrays
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
@Composable
internal fun CurrentLocationPuckApplier(
    currentLocationMode: MapUiSettings.CurrentLocationMode,
    currentMap: MapLibreMap?,
    mapStyle: MapStyle?,
) {
    val activity = LocalContext.current.getActivity()
    val style: Style? = (mapStyle as? MapStyleImpl)?._getStyle()
    style ?: return
    currentMap ?: return
    activity ?: return

    val locationComponent = currentMap.locationComponent

    var hasPermission by remember { mutableStateOf<Boolean?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                hasPermission = if (PermissionsManager.areRuntimePermissionsRequired()) {
                    PermissionsManager.areLocationPermissionsGranted(activity)
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(style, currentLocationMode, hasPermission) {
        when (currentLocationMode) {
            MapUiSettings.CurrentLocationMode.None -> {
                locationComponent.run {
                    if (isLocationComponentActivated) {
                        isLocationComponentEnabled = false
                    }
                }
            }

            MapUiSettings.CurrentLocationMode.Location -> {
                if (hasPermission != true) {
                    askForPermissions(activity)
                    return@LaunchedEffect
                }
                println("after permissions $hasPermission")
                val locationComponentOptions =
                    LocationComponentOptions.builder(activity)
                        .pulseEnabled(false)
                        .build()
                val locationComponentActivationOptions =
                    buildLocationComponentActivationOptions(activity, style, locationComponentOptions)
                locationComponent.activateLocationComponent(locationComponentActivationOptions)
                locationComponent.isLocationComponentEnabled = true
                locationComponent.cameraMode = CameraMode.NONE
                locationComponent.forceLocationUpdate(null)
            }
        }
    }
}

private fun askForPermissions(activity: Activity): Boolean {
    val permissionsManager = PermissionsManager(object : PermissionsListener {
        override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        }

        override fun onPermissionResult(granted: Boolean) {

        }
    })

    return if (PermissionsManager.areLocationPermissionsGranted(activity)) {
        true
    } else {
        permissionsManager.requestLocationPermissions(activity)
        println("perm: Requesting permissions")
        false
    }
}

private fun buildLocationComponentActivationOptions(
    context: Context,
    style: Style,
    locationComponentOptions: LocationComponentOptions
): LocationComponentActivationOptions {
    return LocationComponentActivationOptions
        .builder(context, style)
        .locationComponentOptions(locationComponentOptions)
        .useDefaultLocationEngine(true)
        .locationEngineRequest(
            LocationEngineRequest.Builder(5_000)
                .setFastestInterval(5_000)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .build()
        )
        .build()
}
