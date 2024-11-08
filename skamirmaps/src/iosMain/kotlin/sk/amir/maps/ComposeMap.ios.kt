
package sk.amir.maps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import cocoapods.MapLibre.MLNCoordinateBounds
import cocoapods.MapLibre.MLNMapView
import cocoapods.MapLibre.MLNOrnamentVisibility
import cocoapods.MapLibre.allowsTilting
import platform.CoreGraphics.CGPoint
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import sk.amir.maps.common.LatLng
import sk.amir.maps.common.LatLngBounds

@Composable
internal actual fun NativeComposeMap(
    modifier: Modifier,
    uiSettings: MapUiSettings,
    mapState: MapState,
) {
    mapState as MapStateImpl
    val locationAuthorization = remember {
        mutableStateOf(kCLAuthorizationStatusNotDetermined)
    }
    var mapView: MLNMapView? by remember { mutableStateOf(null) }
    val delegate: MapViewDelegate? by remember(mapState, mapView) {
        mutableStateOf(
            mapView
                ?.let { MapViewDelegate(it, mapState, locationAuthorization) }
        )
    }
    val currentMapView = mapView
    LaunchedEffect(currentMapView, mapState, delegate) {
        currentMapView ?: return@LaunchedEffect
        val delegate = delegate ?: return@LaunchedEffect
        currentMapView.delegate = delegate
        mapState.commandHandle = MapCommandHandleImpl(
            mapState,
            currentMapView,
            delegate
        )
        mapState.emitOnMapLoad()
    }

    DisposableEffect(Unit) {
        onDispose {
            mapState.commandHandle = null
            mapState.style = null
            mapView = null
        }
    }

    LoadUiSettings(uiSettings, mapView, locationAuthorization.value)
    UIKitView(
        factory = {
            MLNMapView()
                .also {
                    it.delegate = delegate
                    mapView = it
                }
        },
        modifier = modifier
            .fillMaxSize(),
        update = {
        }
    )
}

// returns pixels
internal fun CGPoint.toOther(density: Density): Offset {
    return with(density) {
        Offset(x.dp.toPx(), y.dp.toPx())
    }
}

internal fun MLNCoordinateBounds.toOther(): LatLngBounds {
    return LatLngBounds(
        northEast = ne.toOther(),
        southWest = sw.toOther()
    )
}

internal fun CLLocationCoordinate2D.toOther(): LatLng {
    return LatLng(latitude = latitude, longitude = longitude)
}
