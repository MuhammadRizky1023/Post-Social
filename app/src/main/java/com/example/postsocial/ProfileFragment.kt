package com.example.postsocial
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.postsocial.model.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

@Suppress("DEPRECATION")
class ProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var profileImage: CircleImageView
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var userId: String
    private var userListener: ValueEventListener? = null

    companion object {
        private const val GALLERY_REQUEST_CODE = 100
        private const val CAMERA_REQUEST_CODE = 101
        private const val PERMISSION_CAMERA = Manifest.permission.CAMERA
        private const val PERMISSION_READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvName = view.findViewById(R.id.tv_name_profile)
        profileImage = view.findViewById(R.id.profile_image)

        // Get references to Firebase Realtime Database and Firebase Storage
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        // Load user profile information
        loadProfileInfoFromFirebase()

        // Set click listener for profile image
        profileImage.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        return view
    }

    private fun loadProfileInfoFromFirebase() {
        // Get the current user ID
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Load username from Firebase Realtime Database
        val userRef = database.child("users").child(userId)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                user?.let {
                    tvName.text = user.name
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ProfileFragment", "Database error: ${databaseError.message}")
                Toast.makeText(requireContext(), "Failed to load profile data", Toast.LENGTH_SHORT).show()
            }
        })


        loadProfileImageFromFirebase()
    }


    private fun loadProfileImageFromFirebase() {
        val storageRef = storage.reference.child("profile_images").child(userId)
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            // Load the profile image using Glide
            context?.let {
                Glide.with(it)
                    .load(uri)
                    .placeholder(R.drawable.ic_vector_person)
                    .error(R.drawable.ic_vector_person)
                    .into(profileImage)
            }
        }.addOnFailureListener { exception ->
            Log.e("ProfileFragment", "Failed to load profile image: ${exception.message}")
        }
    }

    private fun checkPermissionsAndShowDialog() {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), PERMISSION_CAMERA)
        val storagePermission =
            ContextCompat.checkSelfPermission(requireContext(), PERMISSION_READ_EXTERNAL_STORAGE)

        if (cameraPermission != PackageManager.PERMISSION_GRANTED ||
            storagePermission != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(PERMISSION_CAMERA, PERMISSION_READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            dialogUserSelected()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        val selectedImageBitmap =
                            MediaStore.Images.Media.getBitmap(
                                requireActivity().contentResolver,
                                uri
                            )
                        profileImage.setImageBitmap(selectedImageBitmap)
                        uploadProfileImageToFirebase(selectedImageBitmap)
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    profileImage.setImageBitmap(imageBitmap)
                    uploadProfileImageToFirebase(imageBitmap)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                dialogUserSelected()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permissions Denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun dialogUserSelected() {
        val subjects = arrayOf(
            getString(R.string.from_gallery),
            getString(R.string.take_picture),
            getString(R.string.cancel)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_photo))
            .setItems(subjects) { _, subject ->
                when {
                    subjects[subject] == getString(R.string.from_gallery) -> {
                        playGallery()
                    }
                    subjects[subject] == getString(R.string.take_picture) -> {
                        playTakePhoto()
                    }
                }
            }
            .show()
    }

    private fun playGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    private fun playTakePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.item_option, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.language_menu -> {
                val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                startActivity(intent)
                true
            }
            R.id.logout_menu -> {
                logOutDialogActive()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logOutDialogActive() {
        val builder = AlertDialog.Builder(requireContext())
        builder.apply {
            setTitle(getString(R.string.check_logOut))
            setMessage(getString(R.string.are_you_sure))
            setPositiveButton(getString(R.string.check_no)) { dialog, _ ->
                dialog.dismiss()
            }
            setNegativeButton(getString(R.string.check_yes)) { _, _ ->
                userLogOutNow()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }
    private fun userLogOutNow(){
        userListener?.let {
            FirebaseDatabase.getInstance().reference.removeEventListener(it)
        }

        FirebaseAuth.getInstance().signOut()
        Log.d("Logout", "User successfully logged out")

        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)

        // Tutup aktivitas saat ini agar pengguna tidak dapat kembali ke halaman sebelumnya dengan tombol back
        requireActivity().finish()
    }

    private fun uploadProfileImageToFirebase(bitmap: Bitmap) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$uid")

            // Convert Bitmap to byte array
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            // Upload image to Firebase Storage
            val uploadTask = storageRef.putBytes(data)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                // Get the download URL from the task snapshot
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Update the profile image URL in Firebase Realtime Database
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                    userRef.setValue(User(uid, FirebaseAuth.getInstance().currentUser?.email, tvName.text.toString(), uri.toString()))
                        .addOnSuccessListener {

                        }
                        .addOnFailureListener { exception ->

                        }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to upload profile image: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
