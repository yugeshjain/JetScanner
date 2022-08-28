package com.yugeshjain.bacodescannerjc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yugeshjain.bacodescannerjc.ui.theme.BacodeScannerJCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BacodeScannerJCTheme {
                CameraScreen()
            }
        }
    }
}
