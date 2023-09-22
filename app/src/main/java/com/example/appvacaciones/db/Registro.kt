package com.example.appvacaciones.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Registro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lugar: String,
    val imagenReferencia: String,
    val latitud: Double,
    val longitud: Double,
    val orden: Int,
    val costoAlojamiento: Double,
    val costoTraslado: Double,
    val comentario: String
)