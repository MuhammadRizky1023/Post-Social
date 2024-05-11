package com.example.postsocial

import Post
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.postsocial.adapter.PostAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ListPostFragment : Fragment() {
    private lateinit var  fabPost: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: MutableList<Post>
    private lateinit var databaseReference: DatabaseReference
    private  lateinit var pullRefresh: SwipeRefreshLayout
    private var userListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_list_post, container, false)

        recyclerView = view.findViewById(R.id.rv_list_post)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        postList = mutableListOf()
        postAdapter = PostAdapter()
        recyclerView.adapter = postAdapter


        fabPost = view.findViewById(R.id.fab_post)
        pullRefresh = view.findViewById(R.id.pullRefresh)
        fabPost.setOnClickListener {
            startActivity(Intent(requireActivity(), AddPostActivity::class.java))
        }
        pullRefresh.setOnRefreshListener {
            postAdapter
        }


        val database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference("posts")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in dataSnapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postList.add(post)
                    }
                }
                // Submit the postList to the adapter
                postAdapter.submitList(postList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ListPostFragment", "Database error: ${databaseError.message}")
                Toast.makeText(requireContext(), "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
        return view
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
                dialog.cancel()
            }
            setNegativeButton(getString(R.string.check_yes)) { _, _ ->
                userLogOutNow()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun userLogOutNow(){
        // Menghentikan listener ke Firebase Database
        userListener?.let {
            FirebaseDatabase.getInstance().reference.removeEventListener(it)
        }

        // Logout pengguna dari Firebase Authentication
        FirebaseAuth.getInstance().signOut()
        Log.d("Logout", "User successfully logged out")

        // Mulai aktivitas login setelah logout berhasil
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)

        // Tutup aktivitas saat ini agar pengguna tidak dapat kembali ke halaman sebelumnya dengan tombol back
        requireActivity().finish()
    }


}
