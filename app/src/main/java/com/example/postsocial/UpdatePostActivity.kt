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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream
import java.util.*

@Suppress("DEPRECATION")
class UpdatePostActivity : AppCompatActivity() {
    private lateinit var loading: ProgressBar
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_FROM_GALLERY = 2
    private var selectedImageUri: Uri? = null
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private var userListener: ValueEventListener? = null
    private  lateinit var updateName: TextView
    private lateinit var updateImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_post)

        updateName = findViewById(R.id.tv_update_name)
        val updateDesc = findViewById<EditText>(R.id.ed_update_desc)
        updateImage = findViewById(R.id.iv_update_photo)
        loading = findViewById(R.id.loading_progress_bar)

        // Get post data from intent
        val post = intent.getParcelableExtra<Post>(EXTRA_POST_ID)

        // Check if post data is not null
        post?.let {
            // Fill TextView with username
            updateName.text = post.username
            // Fill EditText with post description
            updateDesc.setText(post.description)

            // Load post image using Glide
            Glide.with(this)
                .load(post.imageUrl)
                .into(updateImage)
        }

        updateImage.setOnClickListener {
            dialogUserSelected()
        }

        val updateBtn = findViewById<Button>(R.id.btn_update)
        updateBtn.setOnClickListener {
            val description = updateDesc.text.toString()
            val postId = post?.postId ?: ""
            if (description.isNotEmpty()) {
                loading.visibility = View.VISIBLE
                updatePostToDatabase(description, postId)
            } else {
                loading.visibility = View.INVISIBLE
                Toast.makeText(this@UpdatePostActivity, "Please fill in the description", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePostToDatabase(description: String, postId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val database = FirebaseDatabase.getInstance()
        val postRef = database.getReference("posts").child(postId)

        val post = Post(
            postId = postId,
            description = description,
            username = updateName.text.toString(), // Menggunakan updateName untuk mendapatkan username
            userId = userId
        )
        if (selectedImageUri != null) {
            val imageRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")
            val uploadTask = imageRef.putFile(selectedImageUri!!)

            uploadTask.addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Tambahkan URL gambar ke post
                    post.imageUrl = uri.toString()

                    // Perbarui postingan di Firebase Realtime Database
                    postRef.setValue(post)
                        .addOnSuccessListener {
                            Toast.makeText(this@UpdatePostActivity, "Post updated successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@UpdatePostActivity, "Failed to update post: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this@UpdatePostActivity, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Jika tidak ada gambar yang dipilih, tetap gunakan URL gambar yang ada di post yang lama
            val oldPostRef = database.getReference("posts").child(postId)
            oldPostRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val oldPost = dataSnapshot.getValue(Post::class.java)
                    oldPost?.let {
                        post.imageUrl = it.imageUrl

                        // Perbarui postingan di Firebase Realtime Database
                        postRef.setValue(post)
                            .addOnSuccessListener {
                                Toast.makeText(this@UpdatePostActivity, "Post updated successfully", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@UpdatePostActivity, "Failed to update post: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@UpdatePostActivity, "Failed to update post: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
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
                        updateImage.setImageURI(selectedImageUri) // Memperbarui updateImage dengan gambar yang dipilih
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        selectedImageUri = getImageUri(this@UpdatePostActivity, imageBitmap)
                        updateImage.setImageBitmap(imageBitmap) // Memperbarui updateImage dengan gambar yang dipilih
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
            .show() // Menampilkan dialog
    }

    private fun userLogOutNow(){
        userListener?.let {
            FirebaseDatabase.getInstance().reference.removeEventListener(it)
        }

        FirebaseAuth.getInstance().signOut()
        Log.d("Logout", "User successfully logged out")

        val intent = Intent(this@UpdatePostActivity, LoginActivity::class.java)
        startActivity(intent)

        // Tutup aktivitas saat ini agar pengguna tidak dapat kembali ke halaman sebelumnya dengan tombol back
        finish()
    }

    companion object {
        const val EXTRA_POST_ID = "postId"
    }
}
