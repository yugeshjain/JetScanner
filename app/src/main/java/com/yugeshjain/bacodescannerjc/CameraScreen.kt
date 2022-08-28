package com.yugeshjain.bacodescannerjc

import android.Manifest
import android.content.pm.PackageManager
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.text.util.LinkifyCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen() {
    val barCodeVal = remember { mutableStateOf("") }
    val context = LocalContext.current
    val mCustomLinkifyText = remember { TextView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        })

    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }
    var previewBoth by remember {
        mutableStateOf<Preview?>(null)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (hasCameraPermission) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        PreviewView(context).apply {
                            this.scaleType = PreviewView.ScaleType.FILL_CENTER
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        val cameraSelector: CameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()
                        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
                        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                            ProcessCameraProvider.getInstance(context)

                        cameraProviderFuture.addListener(
                            {
                                previewBoth = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraProvider: ProcessCameraProvider =
                                    cameraProviderFuture.get()
                                val barcodeAnalyzer = CodeAnalyzer { barcodes ->
                                    barcodes.forEach { barcode ->
                                        barcode.rawValue?.let { barcodeValue ->
                                            barCodeVal.value = barcodeValue
                                        }
                                    }
                                }
                                val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        previewBoth,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    Log.d("TAG", "CameraPreview: ${e.localizedMessage}")
                                }
                            }, ContextCompat.getMainExecutor(context)
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.Red, RectangleShape)
                        .size(
                            if (LocalConfiguration.current.screenWidthDp < LocalConfiguration.current.screenHeightDp) {
                                LocalConfiguration.current.screenWidthDp.times(0.7).dp
                            } else {
                                LocalConfiguration.current.screenHeightDp.times(0.7).dp
                            }
                        )
                        .background(Color.Transparent)
                ) {}
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "QR-Code Text -> ")
                AndroidView(
                    factory = { mCustomLinkifyText }
                ) { textView ->
                    textView.text = barCodeVal.value
                    textView.textSize = 20F
                    LinkifyCompat.addLinks(textView, Linkify.ALL)
                    textView.movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }
    }
}
