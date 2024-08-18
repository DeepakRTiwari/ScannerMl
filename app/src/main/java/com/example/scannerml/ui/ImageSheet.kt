package com.example.scannerml.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.scannerml.R
import java.io.File

@Composable
fun ImageSheet(
    imgList: List<File>?,
    modifier: Modifier = Modifier
) {

    LazyColumn(modifier = modifier.fillMaxSize()) {
        imgList?.let {
            items(it) { file ->
                ImageCardItem(image = file, fileName = file.name, onViewClick = { /*TODO*/ }) {

                }
            }
        }

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
                        Text(text = "View", fontSize = 16.sp)
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}