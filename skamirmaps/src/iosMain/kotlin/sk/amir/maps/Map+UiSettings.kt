
package sk.amir.maps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cocoapods.MapLibre.MLNMapView
import cocoapods.MapLibre.MLNOrnamentVisibility
import cocoapods.MapLibre.MLNUserTrackingModeFollow
import cocoapods.MapLibre.MLNUserTrackingModeNone
import cocoapods.MapLibre.allowsTilting
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse

@Composable
internal fun LoadUiSettings(
    uiSettings: MapUiSettings,
    currentMap: MLNMapView?,
    locationAuthorizationStatus: CLAuthorizationStatus
) {
    LaunchedEffect(uiSettings.isCompassEnabled, currentMap) {
        currentMap?.compassView?.compassVisibility = if (uiSettings.isCompassEnabled) {
            MLNOrnamentVisibility.MLNOrnamentVisibilityAdaptive
        } else {
            MLNOrnamentVisibility.MLNOrnamentVisibilityHidden
        }
    }
    LaunchedEffect(uiSettings.isRotateGesturesEnabled, currentMap) {
        currentMap?.rotateEnabled = uiSettings.isRotateGesturesEnabled
    }
    LaunchedEffect(uiSettings.isTiltGesturesEnabled, currentMap) {
        currentMap?.allowsTilting = uiSettings.isTiltGesturesEnabled
    }
    LaunchedEffect(uiSettings.isZoomGesturesEnabled, currentMap) {
        currentMap?.zoomEnabled = uiSettings.isZoomGesturesEnabled
    }
    LaunchedEffect(uiSettings.isScrollGesturesEnabled, currentMap) {
        currentMap?.scrollEnabled = uiSettings.isScrollGesturesEnabled
    }
    LaunchedEffect(uiSettings.isLogoEnabled, currentMap) {
        currentMap?.logoView?.hidden = !uiSettings.isLogoEnabled
    }
    LaunchedEffect(uiSettings.isAttributionEnabled, currentMap) {
        currentMap?.attributionButton?.hidden = !uiSettings.isAttributionEnabled
    }

    LaunchedEffect(uiSettings.currentLocationMode, currentMap, locationAuthorizationStatus) {
        when (uiSettings.currentLocationMode) {
            MapUiSettings.CurrentLocationMode.None -> {
                currentMap?.showsUserLocation = false
            }
            MapUiSettings.CurrentLocationMode.Location -> {
                if (listOf(kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse).contains(locationAuthorizationStatus)) {
                    currentMap?.showsUserLocation = true
                } else {
                    currentMap?.locationManager?.requestWhenInUseAuthorization()
                }
            }
        }
    }
}
