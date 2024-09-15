package com.example.kidsdrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var ib_brush:ImageButton? = null
    var mImageButtonCurrentPaint:ImageButton? = null
    var ib_Gallery:ImageButton? = null
    var ib_Undo: ImageButton? = null
    lateinit var ib_redo : ImageButton
    lateinit var ib_save : ImageButton

     val openGalleryLauncher: ActivityResultLauncher<Intent> =
         registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
             result->
             if(result.resultCode== RESULT_OK && result.data!=null){
                val imageBackGround : ImageView = findViewById(R.id.iv_background)
                 imageBackGround.setImageURI(result.data?.data)
             }
         }


     val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions->
            permissions.entries.forEach{
                val permissionName  = it.key
                val isGranted = it.value
                if(isGranted){
                    Toast.makeText(this@MainActivity,"Permission granted, now you can read the storage files", Toast.LENGTH_LONG).show()
                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                else {

                        showRationaleDialog("KidsDrawingApp","KidsDrawingApp Needs Permission to access Storage")

                }
            }

        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(10.toFloat())





        ib_brush = findViewById(R.id.ib_brush)
        ib_Gallery = findViewById(R.id.ib_gallery)
        ib_Undo = findViewById(R.id.ib_undo)
        ib_redo = findViewById(R.id.ib_redo)
        ib_save = findViewById(R.id.ib_save)

        ib_brush!!.setOnClickListener{
            showBrushSizeChooserDialog()
        }
        ib_Gallery!!.setOnClickListener{
            requestStoragePermission()

        }
        ib_Undo!!.setOnClickListener{
            undoStroke()
        }
        ib_redo.setOnClickListener{
            redoStroke()
        }
        ib_save.setOnClickListener{
            lifecycleScope.launch{
                 val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_view_container)
                saveBitmapFile(getBitmapFromView(flDrawingView))
            }
        }


    }

    private fun redoStroke() {
        drawingView?.onClickRedo()

    }

    fun undoStroke() {
          drawingView?.onClickUndo()
    }
    fun getBitmapFromView(view: View):Bitmap{
        var bitmap:Bitmap = Bitmap.createBitmap(view.height, view.width, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
           bgDrawable.draw(canvas)
        }
        else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmap

    }

    suspend fun saveBitmapFile(mBitmap: Bitmap) :String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(
                        externalCacheDir?.absoluteFile.toString() + File.separator +
                                "KidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    runOnUiThread {
                        if (result.isNotEmpty()) {
                            Toast.makeText(this@MainActivity, "File saved successfully : $result", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }



    fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialogue_brush_size)
        brushDialog.setTitle("Brush size : ")
        brushDialog.show()

        val smallBtn : ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
                drawingView?.setSizeForBrush(10.toFloat())
               brushDialog.dismiss()
        }
        val mediumBtn : ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()

        }
        val largeBtn : ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()

        }


    }

    fun paintClicked(view: View){
        if(view!=mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            mImageButtonCurrentPaint = imageButton

        }
    }

    private fun showRationaleDialog(title:String, message:String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel"){dialog ,_->
                dialog.dismiss()
            }
            .setPositiveButton("Grant Permission"){ dialog, _->

                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                dialog.dismiss()
            }

        builder.create().show()
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this , Manifest.permission.READ_EXTERNAL_STORAGE)){
           showRationaleDialog("Kids Drawing App", "Kids Drawing App needs to access your external Storage")
        }
        else {
            if(Build.VERSION.SDK_INT>=33){
                requestPermission.launch(arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                    //TODO -  Add writing external Storage Permission
                ))
            }
            else {
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                        //TODO -  Add writing external Storage Permission
                    )
                )
            }
        }

    }



}