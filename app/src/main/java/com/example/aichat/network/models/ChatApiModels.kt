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

data class StreamResponse(
    val is_complete: Boolean,
    val current_step: Int,
    val data: StreamData
)

data class StreamData(
    val message: String? = null, // For loader messages
    val success: Boolean? = null,
    val search_result: String? = null,
    val navigations: List<Navigation>? = null,
    val chat_profile: Profile? = null,
    val chat_context: String? = null
)

data class Navigation(
    val title: String,
    val link: String,
    val icon: String? = null, // Assuming icon is optional
    val description: String
)

data class TrendingQueriesResponse(
    val status: Int,
    val success: Boolean,
    val data: TrendingQueriesData
)

data class TrendingQueriesData(
    val queries: List<TrendingQueryCategory>
)

data class TrendingQueryCategory(
    val category: String,
    val queries: List<String>
)

data class RecentQueriesResponse(
    val status: Int,
    val success: Boolean,
    val data: RecentQueriesData
)

data class RecentQueriesData(
    val queries: List<String>
)


