package com.example.ui.navigation

sealed class Screen(val route: String, val titleFa: String) {
    object Home : Screen("home", "کتابخانه رسانه")
    object Search : Screen("search", "جستجوی معنایی")
    object SearchResults : Screen("search_results", "نتایج جستجو")
    object VideoMoments : Screen("video_moments", "لحظات ویدئو")
    object AudioSearch : Screen("audio_search", "جستجوی صوت")
    object DocumentSearch : Screen("document_search", "جستجوی اسناد")
    object Topics : Screen("topics", "دسته‌بندی موضوعی")
    object IndexingQueue : Screen("indexing_queue", "صف ایندکس")
    object ModelStatus : Screen("model_status", "وضعیت مدل هوش مصنوعی")
    object Diagnostics : Screen("diagnostics", "تنظیمات و عیب‌یابی آفلاین")
}
