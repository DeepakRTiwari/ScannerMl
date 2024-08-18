package com.example.scannerml

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import com.example.scannerml.MainActivity.Companion.REQUIRED_PERMISSIONS
import com.example.scannerml.ui.OnLifecycleEvent
import com.example.scannerml.ui.theme.ScannerMLTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        System.loadLibrary("opencv_java4")

        this.cacheDir?.listFiles()?.forEach {
            if (it.isDirectory) {
                it.listFiles()?.forEach { scannerImage ->
                    Log.d(TAG, "onCreate: $scannerImage")
                }
            }
        }

        setContent {
            ScannerMLTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    RequestPermission(
                        context = this@MainActivity,
                        notGrantedPermission = {
                            // Display the rationale dialog
                            RationalDialog(
                                onDismissRequest = {},
                                positiveButton = {
                                    grantPermissionFromSetting(this@MainActivity)
                                })
                        }) {
                        ScannerScreen()
                    }

                }
            }
        }
    }

    companion object {
        const val TAG = "CameraXApp"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        fun grantPermissionFromSetting(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }
    }
}

@Composable
fun RequestPermission(
    context: Context,
    notGrantedPermission: @Composable () -> Unit,
    grantedPermission: @Composable () -> Unit,
) {

    var isPermissionGranted by remember { mutableStateOf(true) }
    var shouldShowRational by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            var permissionGranted = true

            permission.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value) {
                    permissionGranted = false
                }
            }

            if (!permissionGranted) {
                isPermissionGranted = false
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                isPermissionGranted = true
            }
        }

    LaunchedEffect(key1 = Unit) {
        permissionLauncher.launch(REQUIRED_PERMISSIONS)
        Log.d("TAG", "RequestPermission: Inside Launch Effect")
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                checkPermissions(context).let {
                    isPermissionGranted = it
                    shouldShowRational = !isPermissionGranted
                }
                Log.d("TAG", "RequestPermission: Inside Launch Effec2t")
            }

            else -> {}
        }
    }


    if (isPermissionGranted) {
        grantedPermission()
    }

    if (shouldShowRational) {
        notGrantedPermission()
    }

}

private fun checkPermissions(context: Context): Boolean {
    return REQUIRED_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun RationalDialog(
    onDismissRequest: () -> Unit,
    positiveButton: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    text = "Permission Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    "We need camera permission. Please grant the permission.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = positiveButton
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                /*TextButton(onClick = onDismissRequest) {
                    Text("Cancel", style = TextStyle(color = Color.Black))
                }*/
            },
        )
    }
}
