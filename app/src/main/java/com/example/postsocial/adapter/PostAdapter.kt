package com.example.postsocial.adapter

import Post
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.postsocial.R
import com.example.postsocial.UpdatePostActivity
import com.example.postsocial.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.storage.storage

class PostAdapter : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImageView: ImageView = itemView.findViewById(R.id.profile_image)
        private val usernameTextView: TextView = itemView.findViewById(R.id.tv_name)
        private val postImageView: ImageView = itemView.findViewById(R.id.iv_item_photo)
        private  val descriptionPost: TextView = itemView.findViewById(R.id.tv_description)
        private var currentPost: Post? = null

        fun bind(post: Post) {
            currentPost = post
            usernameTextView.text = post.username
            descriptionPost.text = post.description
            // Load profile image using Glide
            val database = FirebaseDatabase.getInstance()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val userRef = database.getReference("users").child(userId)
            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null) {
                        Glide.with(itemView)
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_vector_person)
                            .into(profileImageView)
                    } else {
                        Log.e("PostAdapter", "User data is null")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("PostAdapter", "Failed to read user data: ${databaseError.message}")
                }
            })


            // Load post image using Glide
            Glide.with(itemView)
                .load(post.imageUrl)
                .placeholder(R.drawable.ic_camera_img)
                .into(postImageView)
        }

        val btnOptions: ImageButton = itemView.findViewById(R.id.btn_options)

        init {
            btnOptions.setOnClickListener {
                currentPost?.let { post ->
                    showPopupMenu(it, post)
                }
            }
        }


        private fun showPopupMenu(view: View, post: Post) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.option_menu, popupMenu.menu)

            val currentUser = FirebaseAuth.getInstance().currentUser
            Log.d("DEBUG", "Current user ID: ${currentUser?.uid}")
            Log.d("DEBUG", "Post user ID: ${post.userId}")

            if (currentUser != null) {
                if (post.userId == currentUser.uid) {
                    // Pengguna saat ini adalah pemilik postingan, jadi tambahkan opsi untuk update dan hapus
                    popupMenu.menu.findItem(R.id.menu_update)?.isVisible = true
                    popupMenu.menu.findItem(R.id.menu_delete)?.isVisible = true
                    // Sembunyikan opsi berbagi jika pengguna adalah pemilik postingan
                    popupMenu.menu.findItem(R.id.share_menu)?.isVisible = false
                } else {
                    // Pengguna saat ini bukan pemilik postingan, jadi sembunyikan opsi untuk update dan hapus
                    popupMenu.menu.findItem(R.id.menu_update)?.isVisible = false
                    popupMenu.menu.findItem(R.id.menu_delete)?.isVisible = false
                    // Tampilkan opsi berbagi jika pengguna bukan pemilik postingan
                    popupMenu.menu.findItem(R.id.share_menu)?.isVisible = true
                }
            } else {
                // Jika tidak ada pengguna yang masuk, sembunyikan semua opsi
                popupMenu.menu.findItem(R.id.menu_update)?.isVisible = false
                popupMenu.menu.findItem(R.id.menu_delete)?.isVisible = false
                popupMenu.menu.findItem(R.id.share_menu)?.isVisible = false
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_update -> {
                        val intent = Intent(view.context, UpdatePostActivity::class.java).apply {
                            putExtra(UpdatePostActivity.EXTRA_POST_ID, post) // Using postId from the clicked item
                        }
                        view.context.startActivity(intent)
                        true
                    }

                    R.id.menu_delete -> {
                        showDeleteConfirmationDialog(post.postId, view.context)
                        true
                    }
                    R.id.share_menu -> {
                        if (currentUser != null && post.userId != currentUser.uid) {
                            sharePost(view.context)
                        } else {
                            Toast.makeText(view.context, "Anda tidak bisa berbagi postingan ini", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }

        private fun sharePost(context: Context) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Bagikan Postingan")
                putExtra(Intent.EXTRA_TEXT, "Teks atau URL Postingan Anda")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Postingan Melalui"))
        }

        private fun showDeleteConfirmationDialog(postId: String, context: Context) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Konfirmasi Hapus")
            builder.setMessage("Apakah Anda yakin ingin menghapus postingan ini?")
            builder.setPositiveButton("Ya") { dialog, which ->
                deletePost(postId)
            }
            builder.setNegativeButton("Tidak") { dialog, which ->
                dialog.dismiss()
            }
            builder.show()
        }

        private fun deletePost(postId: String) {
            val database = Firebase.database
            val postRef = database.reference.child("posts").child(postId)
            postRef.removeValue()
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(itemView.context, "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
