package com.kwancorp.imageupload

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.M
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.SyncStateContract
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.get
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val imageUploadService = Retrofit
        .Builder()
        .baseUrl(BuildConfig.SERVER_URL)
        .build()
        .create(ApiService::class.java)

    private lateinit var imageView: ImageView
    private lateinit var button1: Button
    private var flag = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = ArrayList<Item>()
        for(index in 1..500) {
            val item = Item(index.toLong(), "${index}", "contents")
            list.add(item)
        }
        val rv = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = TempAdapter()
        rv.adapter = adapter
        adapter.submitList(list.toMutableList())

        imageView = findViewById(R.id.imageView)
        button1 = findViewById(R.id.button1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }

        button1.setOnClickListener {
            if(!flag) {
                list.sortByDescending { it.id }
                adapter.submitList(list.toMutableList())
                flag = true
            }else{
                list.sortBy { it.id }
                adapter.submitList(list.toMutableList())
                flag = false
            }
//            val intent = getGalleryIntent()
//            galleryResult.launch(intent)
        }
    }

    /** ????????? ???????????? ???????????? ???????????? */
    private fun getGalleryIntent(): Intent {
        return Intent().also {
            it.action = Intent.ACTION_PICK
            it.type = "image/*"
        }
    }

    /** ?????? ?????? ?????? */
    private val galleryResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            // "content://media/external/images/media/51939" ??????????????? ?????????
            val contentUri = result.data?.data
            contentUri?.let { uri ->
                // content uri??? file path??? ????????????
                val filePath = getFilePathFrom(uri)
                // ????????? ????????? ????????????
                val imageFile = File(filePath)
                // ?????? -> ?????????
                val bitmap = getBitmapFrom(imageFile)
                imageView.setImageBitmap(bitmap)
                uploadImage(bitmap)
            }
        }
    }

    private fun uploadImage(bitmap: Bitmap) {
        try {
            val filesDir = applicationContext.filesDir
            val file = File(filesDir, "${filesDir.name}_${getCurrentDate()}.png")

            // 32byte??? ??? byte ????????? ?????????
            val byteArrayOutputStream = ByteArrayOutputStream()
            // ???????????? ????????? ????????? OutputStream??? ??????(?????? ????????? native ????????? ??????)
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
            // ????????? ????????? byte ????????? ????????????
            val compressedBitmapByteArray = byteArrayOutputStream.toByteArray()

            // ???????????? ?????? ?????? FileOutputStream??? ??????
            val fileOutputStream = FileOutputStream(file)
            // ?????? ?????? ????????? ????????? ????????? ??????
            fileOutputStream.write(compressedBitmapByteArray)
            // OutputStream??? ?????? ????????????
            fileOutputStream.flush()
            // OutputStream??? ?????? ????????? ????????? ????????? ????????????
            fileOutputStream.close()

            // ????????? ????????? ????????? ???????????? RequestBody??? ????????????
            val imageRequestBody = RequestBody.create(MediaType.parse("image/*"), file)
            // form-data??? ????????????
            val formData = MultipartBody.Part.createFormData("upload", file.name, imageRequestBody)
            // ???????????? ???????????? RequestBody??? ????????????
            val textRequestBody = RequestBody.create(MediaType.parse("text/plain"), "upload")

            imageUploadService.uploadImage(
                formData, textRequestBody
            )?.enqueue(object: retrofit2.Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    Log.d("TAG", "onResponse()")
                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    t.printStackTrace()
                }
            })
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /** ?????? -> ????????? */
    private fun getBitmapFrom(file: File): Bitmap {
        // ?????? ??????
        val options = BitmapFactory.Options()
        // ?????? ????????? ?????? ????????? ??????????????? decode??????
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    /** ????????? URI -> ?????? ?????? */
    private fun getFilePathFrom(contentUri: Uri): String {
        var cursor: Cursor? = null
        var filePath = ""

        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = contentResolver.query(contentUri, proj,
                null, null, null)
            cursor?.let { c ->
                c.moveToFirst()
                val columnIndex = c.getColumnIndexOrThrow(
                    MediaStore.Images.Media.DATA)
                filePath = c.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }

        return filePath
    }

    /** ?????? ?????? ?????? */
    private fun getCurrentDate(): String {
        // ?????? ?????? ?????? ??????
        val timeMillis = System.currentTimeMillis()
        // Date???????????? ??????
        val date = Date(timeMillis)
        // ???????????? ?????? ??????  ??????
        val dateFormat = SimpleDateFormat(
            "yyyy_MM_dd_HH_mm_ss", Locale("ko", "KR"))
        // ???????????? ??????
        return dateFormat.format(date)
    }
}