package com.camsense.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Stores the open-port list as JSON text, e.g. "[554,8080]". */
class Converters {
    @TypeConverter fun fromList(v: List<Int>): String = Json.encodeToString(v)
    @TypeConverter fun toList(v: String): List<Int> = Json.decodeFromString(v)
}
