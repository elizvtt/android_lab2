package com.example.lab2_palazova

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var buttonSelfie: Button
    private lateinit var buttonSend: Button

    private var capturedBitmap: Bitmap? = null  // глобальна змінна для збереження фото

    // Реєстрація для результату від камери
    private val takeSelfieResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                // отримуємо фото з інтенту
                val photoBitmap = result.data?.extras?.getParcelable<Bitmap>("data")
                if (photoBitmap != null) {
                    capturedBitmap = photoBitmap
                    imageView.setImageBitmap(photoBitmap) // відображення фото у imageView
                    buttonSend.isEnabled = true // робимо кнопку з надсиланням активною
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ініціалізація елементів UI
        imageView = findViewById(R.id.imageView)
        buttonSelfie = findViewById(R.id.buttonSelfie)
        buttonSend = findViewById(R.id.buttonSend)

        // обробник для кнопки "Зробити селфі"
        buttonSelfie.setOnClickListener {
            // перевірка дозволів
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                // якщо дозволу немає, запитуємо у користувача
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA_PERMISSION)
            }
        }

        // обробник для кнопки "Відправити селфі"
        buttonSend.setOnClickListener {
            // створення інтенту для листа
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("10252100@stud.op.edu.ua")) // кому
                putExtra(Intent.EXTRA_SUBJECT, "ANDROID Палазова АІ221") // тема
                putExtra(Intent.EXTRA_TEXT, "GitHub: https://github.com/elizvtt")

                // додавання зробленого фото
                capturedBitmap?.let { bitmap ->
                    val uri = saveImageToCache(bitmap)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            startActivity(Intent.createChooser(emailIntent, "Виберіть поштовий клієнт"))
        }
    }

    // обробка дозволів
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera() // якщо дозвіл надано відкриваєтся камера
            } else {
                Toast.makeText(this, "Дозвіл на камеру не надано", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // відкриття камери
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takeSelfieResultLauncher.launch(intent)
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1
    }

    // збереження зображення у тимчасове сховище та отримання URI
    private fun saveImageToCache(bitmap: Bitmap): Uri {
        val cachePath = File(externalCacheDir, "images") // каталог для тимчасового збереження
        cachePath.mkdirs()
        val file = File(cachePath, "selfie.png") // файл для збереження
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }
}

