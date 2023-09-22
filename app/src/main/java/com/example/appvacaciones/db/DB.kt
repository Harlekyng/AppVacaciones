package com.example.appvacaciones.db
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

class DB {
    @Database(entities = [Registro::class], version = 1 )
    abstract class DB : RoomDatabase() {
        abstract fun registroDao():RegistroDao

        companion object {
            @Volatile private var BASE_DATOS : DB? = null
            fun getInstance(contexto: Context):DB {
                return BASE_DATOS ?: synchronized(this) {
                    Room.databaseBuilder(
                        contexto.applicationContext,
                        DB::class.java,
                        "RegistrosDB.bd"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                        .also { BASE_DATOS = it } } }
        }
    }
}
