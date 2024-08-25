package com.example.scannerml.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.scannerml.R
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@Composable
fun ImageSheet(
    isSheetVisible: Boolean,
    imgList: List<File>?,
    hideSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    var photoId by remember { mutableStateOf<Uri?>(null) }
    val ctx = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {

        ImageSheetContent(isSheetVisible = isSheetVisible, hideSheet = hideSheet) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imgList?.let {
                    items(it) { file ->
                        val uri = Uri.fromFile(file)
                        ImageCardItem(
                            image = uri,
                            fileName = file.name,
                            onViewClick = { photoId = uri }) {
                            saveImageToStorage(
                                ctx,
                                imageFile = file,
                                filename = file.name,
                            )
                        }
                    }
                }
            }
            photoId?.let {
                ScreamImageViewer(photoId = it, modifier = Modifier.align(Alignment.Center)) {
                    photoId = null
                }
            }
        }
    }
}


@Composable
fun ScreamImageViewer(modifier: Modifier = Modifier, photoId: Uri, onClickScream: () -> Unit) {

    Dialog(onDismissRequest = { onClickScream() }) {
        AsyncImage(
            model = photoId,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(300.dp)
        )
    }
}

@Preview
@Composable
private fun ImageCardItemPreview() {
    ImageCardItem(image = Any(), fileName = "HelloWorld.jpg", onViewClick = { /*TODO*/ }) {

    }
}

@Composable
private fun ImageCardItem(
    image: Any,
    fileName: String,
    onViewClick: () -> Unit,
    onDownloadClick: () -> Unit
) {

    Card(
        modifier = Modifier.heightIn(max = 200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp, 150.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )

            Column(modifier = Modifier.padding(8.dp)) {

                Text(
                    text = fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    color = Color.White
                )

                Spacer(modifier = Modifier.padding(8.dp))

                Row {

                    Button(
                        onClick = onViewClick, modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text(text = "View", fontSize = 12.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.padding(8.dp))

                    Button(
                        onClick = onDownloadClick, modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.downloads),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSheetContent(
    isSheetVisible: Boolean,
    modifier: Modifier = Modifier,
    hideSheet: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {

    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { false })

    val scope = rememberCoroutineScope()

    if (isSheetVisible) {
        ModalBottomSheet(
            modifier = modifier,
            onDismissRequest = { }, sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column {
                Text(
                    text = "Close",
                    textAlign = TextAlign.End,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 18.dp)
                        .fillMaxWidth()
                        .clickable {
                            scope
                                .launch {
                                    sheetState.hide()
                                }
                                .invokeOnCompletion {
                                    hideSheet(false)
                                }
                        },
                )
                content()
            }
        }
    }
}


private fun saveImageToStorage(
    context: Context,
    imageFile: File,  // The file you want to save
    filename: String = "screenshot.jpg",
    mimeType: String = "image/jpeg",
    directory: String = Environment.DIRECTORY_PICTURES + "/ScannerML/",
    mediaContentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
) {

    val imageOutStream: OutputStream?

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, directory)
            }

            val uri = context.contentResolver.insert(mediaContentUri, values)
            imageOutStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val imagePath = Environment.getExternalStoragePublicDirectory(directory).absolutePath
            val image = File(imagePath, filename)
            imageOutStream = FileOutputStream(image)
        }

        // Copy the file to the output stream
        imageFile.inputStream().use { inputStream ->
            imageOutStream?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        Toast.makeText(context, "File Saved", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.d("saveImageToStorage", "Exception: ${e.message}")
        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
    }
}
