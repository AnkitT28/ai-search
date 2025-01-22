package com.example.aichat.network.models

data class ChatRequest(
    val query: String,
    val profiles: List<Profile>,
    val user_id: Int
)

data class Profile(
    val name: String,
    val gender: String,
    val birth_date: Int,
    val birth_month: Int,
    val birth_year: Int,
    val birth_hour: Int,
    val birth_min: Int,
    val birth_lat: String,
    val birth_lon: String,
    val birth_place: String,
    val is_unknown_time: Boolean,
    val birth_timezone: String,
    val is_primary: Boolean
)

data class ChatResponse(
    val status: Int,
    val success: Boolean,
    val data: ResponseData? = null // Make it nullable to handle missing fields
)

data class ResponseData(
    val search_result: String? = null, // Nullable
    val navigations: List<Navigation>? = null,
    val chat_profile: Profile? = null,
    val chat_context: String? = null
)

data class Navigation(
    val title: String,
    val link: String,
    val icon: String,
    val description: String
)
