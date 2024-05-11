package com.example.postsocial.model

data class User(
    val userId: String = "",
    val email: String? = "",
    val name: String? = "",
    val profileImageUrl: String? = ""
)
