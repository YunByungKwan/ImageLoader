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

    /** 앨범을 불러오는 인텐트를 반환한다 */
    private fun getGalleryIntent(): Intent {
        return Intent().also {
            it.action = Intent.ACTION_PICK
            it.type = "image/*"
        }
    }

    /** 앨범 호출 결과 */
    private val galleryResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            // "content://media/external/images/media/51939" 이런식으로 넘어옴
            val contentUri = result.data?.data
            contentUri?.let { uri ->
                // content uri를 file path로 변환한다
                val filePath = getFilePathFrom(uri)
                // 이미지 파일을 생성한다
                val imageFile = File(filePath)
                // 파일 -> 비트맵
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

            // 32byte의 빈 byte 배열을 만든다
            val byteArrayOutputStream = ByteArrayOutputStream()
            // 비트맵의 압축된 버전을 OutputStream에 쓴다(실제 압축은 native 함수가 수행)
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
            // 새롭게 할당된 byte 배열을 가져온다
            val compressedBitmapByteArray = byteArrayOutputStream.toByteArray()

            // 데이터를 쓰기 위한 FileOutputStream을 정의
            val fileOutputStream = FileOutputStream(file)
            // 입력 받은 바이트 배열을 파일에 쓴다
            fileOutputStream.write(compressedBitmapByteArray)
            // OutputStream을 모두 처리한다
            fileOutputStream.flush()
            // OutputStream을 닫고 관련된 시스템 자원을 해제한다
            fileOutputStream.close()

            // 이미지 타입의 파일을 전송하는 RequestBody를 생성한다
            val imageRequestBody = RequestBody.create(MediaType.parse("image/*"), file)
            // form-data를 생성한다
            val formData = MultipartBody.Part.createFormData("upload", file.name, imageRequestBody)
            // 문자열을 전송하는 RequestBody를 생성한다
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

    /** 파일 -> 비트맵 */
    private fun getBitmapFrom(file: File): Bitmap {
        // 기본 설정
        val options = BitmapFactory.Options()
        // 파일 경로에 있는 파일을 비트맵으로 decode한다
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    /** 컨텐츠 URI -> 파일 경로 */
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

    /** 현재 시간 출력 */
    private fun getCurrentDate(): String {
        // 현재 시간 가져 오기
        val timeMillis = System.currentTimeMillis()
        // Date타입으로 변환
        val date = Date(timeMillis)
        // 가져오고 싶은 형태  선언
        val dateFormat = SimpleDateFormat(
            "yyyy_MM_dd_HH_mm_ss", Locale("ko", "KR"))
        // 변환하여 리턴
        return dateFormat.format(date)
    }
}