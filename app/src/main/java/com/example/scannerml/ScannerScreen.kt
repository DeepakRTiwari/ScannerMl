package com.example.scannerml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.scannerml.ui.ImageSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier
) {
    ScannerContent(modifier = modifier)
}

@Composable
fun ScannerContent(modifier: Modifier = Modifier) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var imageBitMap by remember { mutableStateOf<Bitmap?>(null) }
    var savedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var shouldOpenSheet by remember { mutableStateOf(false) }
    var imageList by remember { mutableStateOf<List<File>?>(null) }

    LaunchedEffect(key1 = cameraProviderFuture) {
        imageCapture = ImageCapture.Builder()
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView.scaleType = PreviewView.ScaleType.FILL_START
                val executor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener(
                    {
                        val cameraProvider = cameraProviderFuture.get()

                        //------------------Image Analyzer---------------
                        val opencvAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        opencvAnalyzer.setAnalyzer(executor, LightnessAnalyzer(context = context,
                            savedFileListener = {
                                //we are getting file here of captured image
                            },
                            savedBitmap = {
                                savedBitmap = it
                            }) { frame ->
                            imageBitMap = frame
                        })

                        //----------------------
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                opencvAnalyzer,
                                imageCapture,
                            ).apply {
                                camera = this
                            }

                        } catch (exc: Exception) {
                            Log.d("ScannerML", "${exc.message}")
                        }
                    }, executor
                )
                previewView
            },
            modifier = modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(100.dp)
                .background(Color.White)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {

                Text(
                    text = "Scanned Image",
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    camera?.let {
                        setFlashIcon(it) { isOn ->
                            isFlashOn = isOn
                        }
                    }
                }, modifier = Modifier.size(36.dp)) {
                    Image(
                        painter = painterResource(id = if (isFlashOn) R.drawable.flash else R.drawable.flash_off),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.Black),
                    )
                }
            }
        }

        imageBitMap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                contentScale = ContentScale.Crop
            )
        }

        if (savedBitmap != null) {
            Image(
                bitmap = savedBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clickable {
                        imageList = fetchCacheFile(context)
                        shouldOpenSheet = true
                    }
                    .sizeIn(minWidth = 150.dp, minHeight = 200.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier
                .padding(16.dp)
                .clickable {
                    imageList = fetchCacheFile(context)
                    shouldOpenSheet = true
                }
                .sizeIn(minWidth = 150.dp, minHeight = 200.dp)
                .background(color = Color.Gray)
                .align(Alignment.BottomStart))
        }


        ImageSheet(
            imgList = imageList,
            isSheetVisible = shouldOpenSheet,
            hideSheet = { shouldOpenSheet = false }
        )

    }

}


fun fetchCacheFile(context: Context): List<File> {
    var list = emptyList<File>()
    context.cacheDir.listFiles()?.forEach {
        if (it.isDirectory) {
            list = it.listFiles()?.toList() ?: emptyList()
        }
    }
    return list
}


typealias OpencvListener = (bitmap: Bitmap) -> Unit
typealias SavedFile = (file: File) -> Unit

private class LightnessAnalyzer(
    context: Context,
    private val savedFileListener: SavedFile,
    private val savedBitmap: (Bitmap) -> Unit,
    private val listener: OpencvListener
) : ImageAnalysis.Analyzer {

    private var thresholds = 0
    private var shouldStartCapturing = true
    private val ctx = context

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        image.image?.let { it ->
            if (it.format == ImageFormat.YUV_420_888 && it.planes.size == 3) {
                val frame = it.yuvToRgba()
                val gray = Mat()
                val edges = Mat()

                //Image Processing code goes here!
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGBA2GRAY)

                // Apply Gaussian blur to the grayscale image 5.0
                Imgproc.GaussianBlur(gray, gray, org.opencv.core.Size(5.0, 5.0), 0.0)

                Imgproc.medianBlur(gray, gray, 9)

                // Apply Canny edge detection 75
                Imgproc.Canny(gray, edges, 10.0, 200.0)

                // Apply morphological transformations to close gaps in edges
                val kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_ELLIPSE,
                    org.opencv.core.Size(5.0, 5.0)
                )
                Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, kernel)

                // Apply dilation
                Imgproc.dilate(edges, edges, kernel)

                Imgproc.erode(edges, edges, kernel)

                // Find contours
                val contours = mutableListOf<MatOfPoint>()
                val hierarchy = Mat()
                Imgproc.findContours(
                    edges,
                    contours,
                    hierarchy,
                    Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE
                )

                // Sort contours by area and find the largest one
                contours.sortByDescending { Imgproc.contourArea(it) }

                var documentContour: MatOfPoint? = null

                for (contour in contours) {

                    val area = Imgproc.contourArea(contour)
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(
                        MatOfPoint2f(*contour.toArray()),
                        approx,
                        Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true) * 0.02,
                        true
                    )
                    val points = approx.toArray()

                    if (area > 5000 && points.size == 4) {

                        if (thresholds < 20 && shouldStartCapturing) {
                            thresholds++
                        }


                        if (thresholds == 20 && shouldStartCapturing) {

                            try {

                                shouldStartCapturing = false

                                val srcPoints = orderPoints(points.toList())

                                val dstPoints = findDest(srcPoints.toList())

                                val dstMat = MatOfPoint2f(*dstPoints.first.toTypedArray())
                                val srcMat = MatOfPoint2f(*srcPoints.toTypedArray())

                                // Perform perspective transform
                                val perspectiveTransform =
                                    Imgproc.getPerspectiveTransform(srcMat, dstMat)
                                val warped = Mat()

                                Imgproc.warpPerspective(
                                    frame,
                                    warped,
                                    perspectiveTransform,
                                    org.opencv.core.Size(
                                        dstPoints.second.toDouble(),
                                        dstPoints.third.toDouble()
                                    ),
                                    Imgproc.INTER_LINEAR
                                )

                                Core.flip(warped, warped, 1)

                                val bitmap = Bitmap.createBitmap(
                                    warped.cols(),
                                    warped.rows(),
                                    Bitmap.Config.ARGB_8888
                                )
                                Utils.matToBitmap(warped, bitmap)

                                // Get cache directory
                                val cacheDir = ctx.cacheDir
                                val tempFile = File(cacheDir, "ScannerML_Image")

                                if (!tempFile.exists()) {
                                    tempFile.mkdir()
                                }

                                val file =
                                    File(tempFile.absolutePath, "${System.currentTimeMillis()}.jpg")

                                val outputStream: FileOutputStream

                                try {
                                    outputStream = FileOutputStream(file)
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                    outputStream.flush()
                                    outputStream.close()
                                    // Notify the listener about the saved file
                                    CoroutineScope(Dispatchers.Main).launch {
                                        savedFileListener(file)
                                        savedBitmap(bitmap)
                                        delay(1000)
                                        shouldStartCapturing = true
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                    shouldStartCapturing = true
                                }
                            } catch (_: Exception) {
                            }
                        }

                        documentContour = MatOfPoint(*points)
                        break
                    }
                    thresholds /= 2
                }

                documentContour?.let {

                    //If we found a document contour, fill it with the desired color
                    val contoursList = listOf(it)

                    //Draw straight polyline around image "Line_AA" play's important role here
                    Imgproc.polylines(
                        frame,
                        contoursList,
                        true,
                        Scalar(0.0, 0.0, 255.0, 255.0),
                        1,
                        Imgproc.LINE_AA
                    )

                    // Create a mask
                    val mask = Mat.zeros(frame.size(), frame.type())

                    // Fill the mask with the desired color
                    Imgproc.fillPoly(
                        mask,
                        contoursList,
                        Scalar(0.0, 0.0, 255.0, 255.0)
                    ) // Opaque red color

                    // Blend the mask with the frame
                    Core.addWeighted(
                        frame,
                        1.0,
                        mask,
                        0.5,
                        0.0,
                        frame
                    ) // 0.5 is the transparency factor
                }

                val bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(frame, bmp)

                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }

                val rotatedBitMap = Bitmap.createBitmap(
                    bmp,
                    0,
                    0,
                    image.width,
                    image.height,
                    matrix,
                    true
                )

                listener(rotatedBitMap)
            }
        }
        image.close()
    }
}


private fun setFlashIcon(camera: Camera, isFlashOn: (Boolean) -> Unit) {
    if (camera.cameraInfo.hasFlashUnit()) {
        if (camera.cameraInfo.torchState.value == 0) {
            camera.cameraControl.enableTorch(true)
            isFlashOn(true)
        } else {
            camera.cameraControl.enableTorch(false)
            isFlashOn(false)
        }
    } else {
        //hide flag icon for no flash available
    }
}


fun Image.yuvToRgba(): Mat {
    val rgbaMat = Mat()

    if (format == ImageFormat.YUV_420_888 && planes.size == 3) {
        val chromaPixelStride = planes[1].pixelStride

        if (chromaPixelStride == 2) // chroma channels are interleaved
        {
            assert(planes[0].pixelStride == 1)
            assert(planes[2].pixelStride == 2)
            val yPlane = planes[0].buffer
            val uvPlane1 = planes[1].buffer
            val uvPlane2 = planes[2].buffer

            val yMat = Mat(height, width, CvType.CV_8UC1, yPlane)
            val uvMat1 = Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane1)
            val uvMat2 = Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane2)
            val addrDiff = uvMat2.dataAddr() - uvMat1.dataAddr()

            if (addrDiff > 0) {
                assert(addrDiff == 1L)
                Imgproc.cvtColorTwoPlane(yMat, uvMat1, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV12)
            } else {
                assert(addrDiff == -1L)
                Imgproc.cvtColorTwoPlane(yMat, uvMat2, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21)
            }
        } else // chroma channels are not interleaved
        {
            val yuvBytes = ByteArray(width * (height + height / 2))
            val yPlane = planes[0].buffer
            val uPlane = planes[1].buffer
            val vPlane = planes[2].buffer

            yPlane.get(yuvBytes, 0, width * height)

            val chromaRowStride = planes[1].rowStride
            val chromaRowPadding = chromaRowStride - width / 2

            var offset = width * height

            if (chromaRowPadding == 0) {
                // When the row stride of the chroma channels equals their width, we can copy
                // the entire channels in one go
                uPlane.get(yuvBytes, offset, width * height / 4)
                offset += width * height / 4
                vPlane.get(yuvBytes, offset, width * height / 4)
            } else {
                // When not equal, we need to copy the channels row by row
                for (i in 0 until height / 2) {
                    uPlane.get(yuvBytes, offset, width / 2)
                    offset += width / 2
                    if (i < height / 2 - 1) {
                        uPlane.position(uPlane.position() + chromaRowPadding)
                    }
                }
                for (i in 0 until height / 2) {
                    vPlane.get(yuvBytes, offset, width / 2)
                    offset += width / 2
                    if (i < height / 2 - 1) {
                        vPlane.position(vPlane.position() + chromaRowPadding)
                    }
                }
            }

            val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
            yuvMat.put(0, 0, yuvBytes)
            Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_I420, 4)
        }
    }
    return rgbaMat
}

private fun orderPoints(pts: List<Point>): List<Point> {
    val rect = Array(4) { Point() }

    val sum = pts.map { it.x + it.y }
    val diff = pts.map { it.x - it.y }

    // Top-left point will have the smallest sum
    rect[0] = pts[sum.indexOf(sum.minOrNull()!!)]

    // Bottom-right point will have the largest sum
    rect[2] = pts[sum.indexOf(sum.maxOrNull()!!)]

    // Top-right point will have the smallest difference
    rect[1] = pts[diff.indexOf(diff.minOrNull()!!)]

    // Bottom-left point will have the largest difference
    rect[3] = pts[diff.indexOf(diff.maxOrNull()!!)]

    return rect.toList()
}

private fun findDest(srcPoints: List<Point>): Triple<List<Point>, Int, Int> {
    // Calculate the width and height of the detected document in the source image
    val widthA =
        sqrt((srcPoints[1].x - srcPoints[0].x).pow(2.0) + (srcPoints[1].y - srcPoints[0].y).pow(2.0))
    val widthB =
        sqrt((srcPoints[2].x - srcPoints[3].x).pow(2.0) + (srcPoints[2].y - srcPoints[3].y).pow(2.0))
    val maxWidth = widthA.coerceAtLeast(widthB).toInt()

    val heightA =
        sqrt((srcPoints[2].x - srcPoints[1].x).pow(2.0) + (srcPoints[2].y - srcPoints[1].y).pow(2.0))
    val heightB =
        sqrt((srcPoints[3].x - srcPoints[0].x).pow(2.0) + (srcPoints[3].y - srcPoints[0].y).pow(2.0))
    val maxHeight = heightA.coerceAtLeast(heightB).toInt()

    // Create the destination points array based on the calculated width and height
    val dstPoints = listOf(
        Point(0.0, 0.0),
        Point(maxWidth.toDouble(), 0.0),
        Point(maxWidth.toDouble(), maxHeight.toDouble()),
        Point(0.0, maxHeight.toDouble())
    )

    return Triple(dstPoints, maxWidth, maxHeight)
}
