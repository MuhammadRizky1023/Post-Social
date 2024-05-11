package com.example.postsocial

import Post
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.postsocial.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream

class AddPostActivity : AppCompatActivity() {

    private lateinit var tv_name: TextView
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_FROM_GALLERY = 2
    private var selectedImageUri: Uri? = null
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private var userListener: ValueEventListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        tv_name = findViewById(R.id.tv_name)


        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users").child(it)
            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user = dataSnapshot.getValue(User::class.java)
                    user?.let {
                        val username = user.name
                        tv_name.text = username
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("AddPostActivity", "Database error: ${databaseError.message}")
                }
            })
        }




        val edAddDesc = findViewById<EditText>(R.id.ed_add_desc)
        val btnAdd = findViewById<Button>(R.id.btn_add)
        val addImage = findViewById<ImageView>(R.id.iv_item_photo)
        val loading = findViewById<ProgressBar>(R.id.loading_progress_bar)

        btnAdd.setOnClickListener {
            val description = edAddDesc.text.toString()
            if (description.isNotEmpty()) {
                loading.visibility = View.VISIBLE
                addPostToDatabase(description)
            } else {
                loading.visibility = View.INVISIBLE
                Toast.makeText(this@AddPostActivity, "Please fill in the description", Toast.LENGTH_SHORT).show()
            }
        }

        addImage.setOnClickListener {
            dialogUserSelected()
        }
    }

    private fun addPostToDatabase(description: String) {
        if (selectedImageUri == null) {
            Toast.makeText(this@AddPostActivity, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val database = FirebaseDatabase.getInstance()
        val postRef = database.getReference("posts")

        // Buat objek Post dan atur nilainya
        val post = Post(
            description = description,
            username = tv_name.text.toString(),
            userId = userId
        )

        // Simpan gambar ke Firebase Storage
        val imageRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")
        val uploadTask = imageRef.putFile(selectedImageUri!!)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                // Tambahkan URL gambar ke post
                post.imageUrl = uri.toString()

                // Kirim post ke Firebase Realtime Database
                val postId = postRef.push().key
                postId?.let {
                    // Gunakan postId sebagai child untuk menyimpan post
                    post.postId = it // Set postId pada objek post
                    postRef.child(it).setValue(post)
                        .addOnSuccessListener {
                            Toast.makeText(this@AddPostActivity, "Post added successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@AddPostActivity, "Failed to add post: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this@AddPostActivity, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }







    private fun dialogUserSelected() {
        val subjects = arrayOf<CharSequence>(
            getString(R.string.from_gallery),
            getString(R.string.take_picture),
            getString(R.string.cancel)
        )

        val header = TextView(this).apply {
            text = getString(R.string.select_photo)
            gravity = android.view.Gravity.CENTER
            setPadding(10, 15, 15, 10)
            setTextColor(resources.getColor(R.color.dark_blue))
            textSize = 22f
        }

        AlertDialog.Builder(this).apply {
            setCustomTitle(header)
            setItems(subjects) { dialog, subject ->
                when (subjects[subject]) {
                    getString(R.string.from_gallery) -> playGallery()
                    getString(R.string.take_picture) -> playTakePhoto()
                    getString(R.string.cancel) -> dialog.dismiss()
                }
            }
        }.show()
    }

    private fun playGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_IMAGE_FROM_GALLERY)
    }

    private fun playTakePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_FROM_GALLERY -> {
                    selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        val addImage = findViewById<ImageView>(R.id.iv_item_photo)
                        addImage.setImageURI(selectedImageUri)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        selectedImageUri = getImageUri(this@AddPostActivity, imageBitmap)
                        val addImage = findViewById<ImageView>(R.id.iv_item_photo)
                        addImage.setImageBitmap(imageBitmap)
                    }
                }
            }
        }
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }



    override fun onCreateOptionsMenu(itemMenu: Menu): Boolean {
        val menu = menuInflater
        menu.inflate(R.menu.item_option, itemMenu)
        return super.onCreateOptionsMenu(itemMenu)
    }

    override fun onOptionsItemSelected(subjectItem: MenuItem): Boolean {
        return  when(subjectItem.itemId){
            R.id.language_menu -> {
                val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                startActivity(intent)
                true
            }
            R.id.logout_menu -> {
                logOutDialogActive()
                true
            }

            else -> true
        }
    }

    private  fun logOutDialogActive(){
        val maker= AlertDialog.Builder(this)
        val dialog = maker.create()
        maker
            .setTitle(getString(R.string.check_logOut))
            .setMessage(getString(R.string.are_you_sure))
            .setPositiveButton(getString(R.string.check_no)){_, _ ->
                dialog.cancel()
            }
            .setNegativeButton(getString(R.string.check_yes)){_, _ ->
                userLogOutNow()
            }
    }

    private fun userLogOutNow(){
        userListener?.let {
            FirebaseDatabase.getInstance().reference.removeEventListener(it)
        }

        FirebaseAuth.getInstance().signOut()
        Log.d("Logout", "User successfully logged out")

        val intent = Intent(this@AddPostActivity, LoginActivity::class.java)
        startActivity(intent)

        // Tutup aktivitas saat ini agar pengguna tidak dapat kembali ke halaman sebelumnya dengan tombol back
        finish()
    }


}
