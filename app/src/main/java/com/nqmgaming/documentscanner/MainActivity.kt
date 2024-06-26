package com.nqmgaming.documentscanner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.nqmgaming.documentscanner.ui.theme.DocumentScannerTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(RESULT_FORMAT_PDF, RESULT_FORMAT_JPEG)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            DocumentScannerTheme {
                var images by remember {
                    mutableStateOf<List<Uri>>(emptyList())
                }

                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { activityResult ->
                        try {
                            if (activityResult.resultCode == RESULT_OK && activityResult.data != null) {
                                val result = GmsDocumentScanningResult.fromActivityResultIntent(
                                    activityResult.data!!
                                )

                                if (result != null) {
                                    images = (result.pages?.map {
                                        it.imageUri
                                    } ?: emptyList())

                                    // Save the images to the app's directory
                                  images.map { uri ->
                                        val file = File(filesDir, uri.lastPathSegment!!)
                                        val inputStream = contentResolver.openInputStream(uri)
                                        val outputStream = FileOutputStream(file)
                                        inputStream?.copyTo(outputStream)
                                        file
                                    }

                                    contentResolver.openInputStream(result.pdf?.uri!!)
                                        ?.use { inputStream ->
                                            val outputStream =
                                                FileOutputStream(File(filesDir, "document.pdf"))
                                            inputStream.copyTo(outputStream)
                                        }
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to get result",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            }

                        }catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Error handling result: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Document Scanner",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 20.sp
                                ),
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(it)
                                                .setFillInIntent(null)
                                                .build()

                                        )
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Failed to start scanner",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        ) {
                            Text("Scan")
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        images.forEach { image ->
                            AsyncImage(
                                model = image,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }

                        if (images.isEmpty()) {
                            Text("No images to display")
                        }

                    }
                }
            }
        }
    }
}

