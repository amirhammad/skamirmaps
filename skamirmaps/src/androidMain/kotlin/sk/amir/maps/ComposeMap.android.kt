
package sk.amir.maps

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.ContextWrapper
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import sk.amir.maps.common.CameraPadding
import sk.amir.maps.common.CameraUpdate
import sk.amir.maps.common.LatLng
import sk.amir.maps.common.LatLngBounds
import sk.amir.maps.helpers.getActivity
import kotlin.coroutines.suspendCoroutine

@Composable
internal actual fun NativeComposeMap(
    modifier: Modifier,
    uiSettings: MapUiSettings,
    mapState: MapState,
) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(context) {
        appContext = context
    }

    mapState as MapStateImpl
    var map: MapLibreMap? by remember { mutableStateOf(null) }
    var mapView: MapView? by remember { mutableStateOf(null) }

    LaunchedEffect(mapView) {
        map = mapView?.getMapAsync()
    }

    val density = LocalDensity.current
    val currentMapView = mapView
    val currentMap = map
    LaunchedEffect(currentMapView, currentMap, mapState, density) {
        currentMapView ?: return@LaunchedEffect
        currentMap ?: return@LaunchedEffect
        mapState.commandHandle = MapCommandHandleImpl(
            mapView = currentMapView,
            mapState = mapState,
            map = currentMap,
            density = density
        )
        mapState.emitOnMapLoad()
        // we don't need to emit immediately camera position (it would be just zeroes)
        currentMap.addOnCameraIdleListener {
            mapState.emitOnCameraDidChange(currentMap.cameraPosition.toOther(density))
        }
    }

    InitializeMap()
    mapView?.let { RegisterActivityCallbacksForMapView(it) }
    LoadUiSettings(uiSettings, currentMap, mapState)

    AndroidView(
        factory = { context ->
            MapView(context)
                .also {
                    it.attachListenersToCommand(mapState) { map }
                    mapView = it
                }
        },
        modifier = modifier,
        update = {

        }
    )
}

private fun MapView.attachListenersToCommand(mapState: MapStateImpl, map: () -> MapLibreMap?) {
    addOnDidFinishLoadingStyleListener {
        val style = map()?.style ?: return@addOnDidFinishLoadingStyleListener
        println("loaded style ${style.isFullyLoaded} ${style.uri}")
        mapState.style = MapStyleImpl(style)
        mapState.emitOnStyleFinishedLoading()
    }
    addOnStyleImageMissingListener { imageName ->
        if (imageName.isNotBlank()) {
            println("image is missing: '$imageName'")
        }
    }
}

private class MapCommandHandleImpl(
    private val mapView: MapView,
    private val mapState: MapStateImpl,
    private val map: MapLibreMap,
    private val density: Density,
): MapCommandHandle, InternalMapCommandHandle {
    override fun loadStyleUri(styleUri: String) {
        mapState.clearStyleLoaded()
        map.setStyle(Style.Builder().fromUri(styleUri))
    }

    override fun projectLatLngToVisibleArea(latLng: LatLng, density: Density): Offset {
        return map.projection.toScreenLocation(latLng.toOther()).let {
            Offset(it.x, it.y)
        }
    }

    override fun projectOffsetToLatLng(offset: Offset, density: Density): LatLng {
        return map.projection.fromScreenLocation(
            offset.let { PointF(it.x, it.y) }
        ).let { LatLng(it.latitude, it.longitude) }
    }

    private fun CameraUpdate.toNativeCameraUpdate(): org.maplibre.android.camera.CameraUpdate = when (this) {
        is CameraUpdate.TargetZoom -> {
            CameraUpdateFactory.newLatLngZoom(
                latLng = target.toOther(),
                zoom = zoom,
            )
        }

        is CameraUpdate.BoundsPadding -> with(density) {
            CameraUpdateFactory.newLatLngBounds(
                bounds = org.maplibre.android.geometry.LatLngBounds.fromLatLngs(
                    listOf(
                        bounds.northEast.toOther(),
                        bounds.southWest.toOther(),
                    )
                ),
                paddingLeft = padding.start.toPx().toInt(), // RTL bug
                paddingTop = padding.top.toPx().toInt(),
                paddingRight = padding.end.toPx().toInt(), // RTL bug
                paddingBottom = padding.bottom.toPx().toInt()
            )
        }

        is CameraUpdate.Target -> {
            CameraUpdateFactory.newLatLng(target.toOther())
        }
    }


    override fun updateCamera(cameraUpdate: CameraUpdate) {
        val update = cameraUpdate.toNativeCameraUpdate()
        if (cameraUpdate.animated) {
            map.animateCamera(update)
        } else {
            map.moveCamera(update)
        }
    }

    override fun getVisibleArea(): LatLngBounds {
        return map.projection.visibleRegion.latLngBounds.toOther()
    }
}

internal fun org.maplibre.android.geometry.LatLngBounds.toOther(): LatLngBounds {
    return LatLngBounds(
        northEast = northEast.toOther(),
        southWest = southWest.toOther(),
    )
}

internal fun org.maplibre.android.geometry.LatLng.toOther(): LatLng {
    return LatLng(
        latitude = latitude,
        longitude = longitude
    )
}

private fun CameraPosition.toOther(density: Density): sk.amir.maps.common.CameraPosition {
    return sk.amir.maps.common.CameraPosition(
        target = target?.let {
            LatLng(it.latitude, it.longitude)
        } ?: LatLng(0.0, 0.0),
        zoom = zoom,
        bearing = bearing,
        padding = getCameraPadding(density) ?: CameraPadding(0.0.dp),
        tilt = tilt
    )
}

internal suspend fun MapView.getMapAsync(): MapLibreMap {
    return suspendCoroutine { continuation ->
        getMapAsync { loadedMap: MapLibreMap? ->
            check(loadedMap != null) { "Unable to load map" }
            continuation.resumeWith(Result.success(loadedMap))
        }
    }
}

private fun CameraPadding.toOther(): PaddingValues {
    return PaddingValues(
        start = start,
        end = end,
        top = top,
        bottom = bottom
    )
}

private fun CameraPadding.toDoubleArray(): DoubleArray {
    return arrayOf(
        start.value.toDouble(),
        top.value.toDouble(),
        end.value.toDouble(),
        bottom.value.toDouble()
    ).toDoubleArray()
}

private fun CameraPosition.getCameraPadding(density: Density): CameraPadding? = with(padding) {
    this ?: return null
    return density.run {
        CameraPadding(
            start = get(0).toFloat().toDp(),
            top = get(1).toFloat().toDp(),
            end = get(2).toFloat().toDp(),
            bottom = get(3).toFloat().toDp()
        )
    }
}

@Composable
private fun RegisterActivityCallbacksForMapView(mapView: MapView) {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        DisposableEffect(Unit) {
            val callbacks = MapLifecycleCallbacks(mapView)
            val activity = context.getActivity() ?: return@DisposableEffect onDispose {  }
            activity.registerActivityLifecycleCallbacks(callbacks)
            onDispose {
                activity.unregisterActivityLifecycleCallbacks(callbacks)
            }
        }
    }
}

@Composable
private fun InitializeMap() {
    val context = LocalContext.current
    MapLibre.getInstance(context)
}

private class MapLifecycleCallbacks(private val mapView: MapView) : ActivityLifecycleCallbacks {
    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
    override fun onActivityDestroyed(p0: Activity) {}

    override fun onActivityPaused(p0: Activity) {
        mapView.onPause()
    }

    override fun onActivityResumed(p0: Activity) {
        mapView.onResume()
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        mapView.onSaveInstanceState(p1)
    }

    override fun onActivityStarted(p0: Activity) {
        mapView.onStart()
    }

    override fun onActivityStopped(p0: Activity) {
        mapView.onStop()
    }
}