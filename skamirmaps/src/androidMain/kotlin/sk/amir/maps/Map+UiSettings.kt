
package sk.amir.maps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Camera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.permissions.PermissionsListener
import org.maplibre.android.location.permissions.PermissionsManager
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import sk.amir.maps.helpers.getActivity
import kotlin.coroutines.suspendCoroutine

@SuppressLint("MissingPermission")
@Composable
internal fun LoadUiSettings(
    uiSettings: MapUiSettings,
    currentMap: MapLibreMap?,
    mapState: MapState,
) {
    LaunchedEffect(uiSettings.isCompassEnabled, currentMap) {
        currentMap?.uiSettings?.isCompassEnabled = uiSettings.isCompassEnabled
    }
    LaunchedEffect(uiSettings.isRotateGesturesEnabled, currentMap) {
        currentMap?.uiSettings?.isRotateGesturesEnabled = uiSettings.isRotateGesturesEnabled
    }
    LaunchedEffect(uiSettings.isTiltGesturesEnabled, currentMap) {
        currentMap?.uiSettings?.isTiltGesturesEnabled = uiSettings.isTiltGesturesEnabled
    }
    LaunchedEffect(uiSettings.isZoomGesturesEnabled, currentMap) {
        currentMap?.uiSettings?.isZoomGesturesEnabled = uiSettings.isZoomGesturesEnabled
    }
    LaunchedEffect(uiSettings.isScrollGesturesEnabled, currentMap) {
        currentMap?.uiSettings?.isScrollGesturesEnabled = uiSettings.isScrollGesturesEnabled
    }
    LaunchedEffect(uiSettings.isLogoEnabled, currentMap) {
        currentMap?.uiSettings?.isLogoEnabled = uiSettings.isLogoEnabled
    }
    LaunchedEffect(uiSettings.isAttributionEnabled, currentMap) {
        currentMap?.uiSettings?.isAttributionEnabled = uiSettings.isAttributionEnabled
    }

    CurrentLocationPuckApplier(
        currentLocationMode = uiSettings.currentLocationMode,
        currentMap = currentMap,
        mapStyle = mapState.style,
    )
}
