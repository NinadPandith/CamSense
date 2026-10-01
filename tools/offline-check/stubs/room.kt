@file:Suppress("unused")
package androidx.room
import kotlin.reflect.KClass
annotation class Entity(val tableName: String = "", val foreignKeys: Array<ForeignKey> = [], val indices: Array<Index> = [])
annotation class ColumnInfo(val name: String = "")
annotation class PrimaryKey(val autoGenerate: Boolean = false)
annotation class ForeignKey(val entity: KClass<*>, val parentColumns: Array<String>, val childColumns: Array<String>, val onDelete: Int = 1) {
    companion object { const val CASCADE = 5; const val SET_NULL = 3; const val NO_ACTION = 1 }
}
annotation class Index(vararg val value: String, val unique: Boolean = false)
annotation class Dao
annotation class Insert
annotation class Update
annotation class Query(val value: String)
annotation class Transaction
annotation class Embedded
annotation class Relation(val parentColumn: String, val entityColumn: String)
annotation class Database(val entities: Array<KClass<*>>, val version: Int, val exportSchema: Boolean = true)
annotation class TypeConverters(vararg val value: KClass<*>)
annotation class TypeConverter
abstract class RoomDatabase {
    val openHelper: androidx.sqlite.db.SupportSQLiteOpenHelper get() = TODO()
    class Builder<T : RoomDatabase> { fun build(): T = TODO() }
}
object Room { fun <T : RoomDatabase> databaseBuilder(context: android.content.Context, klass: Class<T>, name: String): RoomDatabase.Builder<T> = TODO() }
suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R = block()
