package com.agriedge.app.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.agriedge.app.config.AppConfig.Lang
import com.agriedge.app.i18n.Strings
import com.agriedge.app.ui.theme.Forest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    lang: Lang,
    onImageCaptured: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val permission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }

    if (permission.status.isGranted) {
        CameraPreviewContent(lang = lang, onImageCaptured = onImageCaptured, onBack = onBack)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(Strings.t(lang, "camera_permission"))
            Spacer(Modifier.height(16.dp))
            PrimaryButton(Strings.t(lang, "grant_permission"), onClick = { permission.launchPermissionRequest() })
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) { Text(Strings.t(lang, "cancel")) }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    lang: Lang,
    onImageCaptured: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Framing guide
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
        )

        // Hint
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.55f)
        ) {
            Text(
                text = Strings.t(lang, "camera_hint"),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = Strings.t(lang, "cancel"), tint = Color.White)
        }

        // Shutter
        IconButton(
            onClick = { takePhoto(context, imageCapture, onImageCaptured) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, Forest, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Forest)
            )
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                image.close()
                if (bitmap != null) onImageCaptured(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        }
    )
}
