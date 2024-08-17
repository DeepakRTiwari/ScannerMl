package com.example.scannerml

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RationalDialog(
    onDismissRequest: () -> Unit ,
    navigateToSetting: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {

        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {

            Text(
                text = "Camera permission is necessary to proceed with this app. Please grant permission to proceed further.",
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Cancel",
                    color = Color.Black,
                    modifier = Modifier.clickable {
                        onDismissRequest()
                    }
                )
                Text(
                    text = "OK",
                    color = Color.Black,
                    modifier = Modifier.clickable {
                        navigateToSetting()
                    }
                )
            }
        }
    }
}


