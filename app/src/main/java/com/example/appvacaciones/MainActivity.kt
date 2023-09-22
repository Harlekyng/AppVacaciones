package com.example.appvacaciones

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberImagePainter
import com.example.appvacaciones.db.DB
import com.example.appvacaciones.db.Registro
import com.example.appvacaciones.db.RegistroDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.material3.Text as Text1

enum class Pantalla {
    FORMULARIO,
    INGRESO,
    EDITAR,
    VISUALIZAR
}

class CameraAppViewModel : ViewModel() {
    val pantalla = mutableStateOf(Pantalla.FORMULARIO)
    var onPermisoCamaraOk: () -> Unit = {}
    var onPermisoUbicacionOk: () -> Unit = {}
    var lanzadorPermisos: ActivityResultLauncher<Array<String>>? = null

    fun PantallaEditarRegistro(registro: Registro) {
        val registroDefault = registro
        pantalla.value = Pantalla.EDITAR
    }

    fun cambiarPantalla1() {
        pantalla.value = Pantalla.INGRESO
    }

    fun cambiarPantalla2Formulario() {
        pantalla.value = Pantalla.FORMULARIO
    }

    fun visualizarImagenCompleta(){
        pantalla.value = Pantalla.VISUALIZAR
    }
}

class MainActivity : ComponentActivity() {
    val cameraAppVm: CameraAppViewModel by viewModels()
    val lanzadorPermisos = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            (permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
                    (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false) -> {
                Log.v("callback RequestMultiplePermissions", "Permiso de ubicación concedido")
                cameraAppVm.onPermisoUbicacionOk()
            }
            else -> {
                Log.v("callback RequestMultiplePermissions", "Permiso de ubicación no concedido")
            }
        }
    }

    private lateinit var registroDao: RegistroDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            registroDao = DB.DB.getInstance(this@MainActivity).registroDao()
            val registrosDb = registroDao.getAllRegistros()

            withContext(Dispatchers.Main) {
                cameraAppVm.cambiarPantalla2Formulario()
                cameraAppVm.lanzadorPermisos = lanzadorPermisos
                setContent {
                    VacationApp(
                        cameraAppViewModel = cameraAppVm,
                        registroDao = registroDao
                    )
                }
            }
        }
    }
}

@Composable
fun VacationApp(
    onSave: () -> Unit = {},
    cameraAppViewModel: CameraAppViewModel,
    registroDao: RegistroDao
) {
    val (registros) = remember { mutableStateOf(emptyList<Registro>()) }
    val miCorrutina = rememberCoroutineScope()

    when (cameraAppViewModel.pantalla.value) {
        Pantalla.FORMULARIO -> {
            ListadoDeRegistros(
                appViewModel = cameraAppViewModel,
                registros = registros,
                onEliminarClick = { registro ->
                    miCorrutina.launch {
                        registroDao.eliminarRegistro(registro)
                        cameraAppViewModel.cambiarPantalla2Formulario()
                    }
                },
                onEditarClick = { registro ->
                    cameraAppViewModel.PantallaEditarRegistro(registro)
                }
            )
        }

        Pantalla.INGRESO -> {
            PantallaRegistroDeDatos(
                appViewModel = cameraAppViewModel,
                registroDao = registroDao,
                onGuardarClick = { nuevoRegistro ->
                    miCorrutina.launch {
                        registroDao.insertarRegistro(nuevoRegistro)
                        cameraAppViewModel.cambiarPantalla2Formulario()
                    }
                }
            )
        }

        Pantalla.EDITAR -> {
            val registroDefault = Registro(
                lugar = "Nombre del lugar",
                imagenReferencia = "",
                latitud = 000.000,
                longitud = 000.000,
                orden = 0,
                costoAlojamiento = 000.0,
                costoTraslado = 000.0,
                comentario = ""
            )

            PantallaEditar(
                appViewModel = cameraAppViewModel,
                registro = registroDefault,
                onActualizarClick = { editedRegistro ->
                    miCorrutina.launch {
                        registroDao.actualizarRegistro(editedRegistro)
                        cameraAppViewModel.cambiarPantalla2Formulario()
                    }
                }
            )
        }

        Pantalla.VISUALIZAR -> {
            val registroDefault = Registro(
                lugar = "No ha Ingresado Lugar",
                imagenReferencia = "https://img.freepik.com/vector-premium/cara-caja-404_114341-17.jpg?w=900",
                latitud = 00.0,
                longitud = 00.0,
                orden = 0,
                costoAlojamiento = 00000.0,
                costoTraslado = 00000.0,
                comentario = "Parece que no has ingresado ningun Lugar aun llena el formulario"
            )

            DetalleDelRegistro(
                appViewModel = cameraAppViewModel,
                registro = registroDefault!!,
                onEliminarClick = { registro ->
                    miCorrutina.launch {
                        cameraAppViewModel.cambiarPantalla2Formulario()
                        onSave()
                    }
                },
                onEditarClick = { registro ->
                    cameraAppViewModel.PantallaEditarRegistro(registro)
                }
            )
        }
        else -> {}
    }
}
@Composable
fun ListadoDeRegistros(
    registros: List<Registro>,
    onEliminarClick: (Registro) -> Unit,
    onEditarClick: (Registro) -> Unit,
    appViewModel: CameraAppViewModel
) {
    val contexto = LocalContext.current
    val (registros, setRegistros) = remember { mutableStateOf(emptyList<Registro>()) }

    LaunchedEffect(registros) {
        withContext(Dispatchers.IO) {
            val dao = DB.DB.getInstance(contexto).registroDao()
            setRegistros(dao.getAllRegistros())
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
        ) {
            items(registros) { registro ->
                RegistroEnFormulario(
                    registro = registro,
                    onEliminarClick = onEliminarClick,
                    cameraAppViewModel = appViewModel,
                ) {
                    setRegistros(emptyList<Registro>())
                }
            }
        }

        Button(
            onClick = {
                appViewModel.cambiarPantalla1()
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar Lugar",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text1("Agregar Lugar de Vacacion")
            }
        }
    }
}

@Composable
fun RegistroEnFormulario(
    registro: Registro,
    onSave: () -> Unit = {},
    onEliminarClick: (Registro) -> Unit,
    cameraAppViewModel: CameraAppViewModel,
    function: () -> Unit
) {
    val contexto = LocalContext.current
    val miCorrutina = rememberCoroutineScope()
    val (registros, setRegistros) = remember { mutableStateOf(emptyList<Registro>()) }

    Column(
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
            .padding(5.dp)
            .clickable {
                cameraAppViewModel.visualizarImagenCompleta()
            }
            .background(Color.White)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val painter = rememberImagePainter(
                data = registro.imagenReferencia,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.error)
                }
            )

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .offset(y = -4.dp)
                    .fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                Text1(
                    text = "${registro.lugar}",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 0.dp).offset(y = -5.dp)
                )
                Spacer(modifier = Modifier.height(0.dp))

                Text1(
                    text = "Costo Alojamiento: ${registro.costoAlojamiento}",
                    style = TextStyle(fontSize = 10.sp),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text1(
                    text = "Costo Traslado: ${registro.costoTraslado}",
                    style = TextStyle(fontSize = 10.sp),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier
                            .clickable {
                                cameraAppViewModel.PantallaEditarRegistro(registro)
                            }
                    )

                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar Registro",
                        modifier = Modifier
                            .clickable {
                                onEliminarClick(registro)
                                miCorrutina.launch(Dispatchers.IO) {
                                    val dao = DB.DB.getInstance(contexto).registroDao()
                                    dao.eliminarRegistro(registro)
                                    onSave()
                                }
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditar(
    onSave: () -> Unit = {},
    appViewModel: CameraAppViewModel,
    registro: Registro,
    onActualizarClick: (Registro) -> Unit
) {
    var editedLugar by remember { mutableStateOf(registro.lugar) }
    var editedImagenReferencia by remember { mutableStateOf(registro.imagenReferencia) }
    var editedLatitud by remember { mutableStateOf(registro.latitud.toString()) }
    var editedLongitud by remember { mutableStateOf(registro.longitud.toString()) }
    var editedOrden by remember { mutableStateOf(registro.orden.toString()) }
    var editedCostoAlojamiento by remember { mutableStateOf(registro.costoAlojamiento.toString()) }
    var editedCostoTraslado by remember { mutableStateOf(registro.costoTraslado.toString()) }
    var editedComentario by remember { mutableStateOf(registro.comentario) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.width(16.dp))

        TextField(
            value = editedLugar,
            onValueChange = { editedLugar = it },
            label = { Text1("Lugar") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.width(16.dp))

        TextField(
            value = editedImagenReferencia,
            onValueChange = { editedImagenReferencia = it },
            label = { Text1("Imagen de Referencia") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.width(16.dp))

        TextField(
            value = editedLatitud,
            onValueChange = { editedLatitud = it },
            label = { Text1("Latitud") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.width(16.dp))

        TextField(
            value = editedLongitud,
            onValueChange = { editedLongitud = it },
            label = { Text1("Longitud") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.width(16.dp))

        TextField(
            value = editedOrden,
            onValueChange = { editedOrden = it },
            label = { Text1("Orden") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.width(16.dp))

        TextField(
            value = editedCostoAlojamiento,
            onValueChange = { editedCostoAlojamiento = it },
            label = { Text1("Costo Alojamiento") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.width(16.dp))

        TextField(
            value = editedCostoTraslado,
            onValueChange = { editedCostoTraslado = it },
            label = { Text1("Costo Traslado") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.width(16.dp))

        TextField(
            value = editedComentario,
            onValueChange = { editedComentario = it },
            label = { Text1("Comentarios") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = {
                val editedRegistro = Registro(
                    lugar = editedLugar,
                    imagenReferencia = editedImagenReferencia,
                    latitud = editedLatitud.toDoubleOrNull() ?: 0.0,
                    longitud = editedLongitud.toDoubleOrNull() ?: 0.0,
                    orden = editedOrden.toIntOrNull() ?: 0,
                    costoAlojamiento = editedCostoAlojamiento.toDoubleOrNull() ?: 0.0,
                    costoTraslado = editedCostoTraslado.toDoubleOrNull() ?: 0.0,
                    comentario = editedComentario
                )
                onActualizarClick(editedRegistro)
                onSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 16.dp)
        ) {
            Text1("Actualizar Registro")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                appViewModel.cambiarPantalla2Formulario()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
        ) {
            Text1("Volver")
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistroDeDatos(
    onSave: () -> Unit = {},
    appViewModel: CameraAppViewModel,
    registroDao: RegistroDao,
    onGuardarClick: (Registro) -> Unit
) {
    var lugar by remember { mutableStateOf("") }
    var imagenReferencia by remember { mutableStateOf("") }
    var latitud by remember { mutableStateOf(0.0) }
    var longitud by remember { mutableStateOf(0.0) }
    var orden by remember { mutableStateOf(0) }
    var costoAlojamiento by remember { mutableStateOf(0.0) }
    var costoTraslado by remember { mutableStateOf(0.0) }
    var comentario by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = lugar,
            onValueChange = { lugar = it },
            label = { Text1("Lugar") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = imagenReferencia,
            onValueChange = { imagenReferencia = it },
            label = { Text1("Imagen de Referencia (URL)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = latitud.toString(),
            onValueChange = { latitud = it.toDoubleOrNull() ?: 0.0 },
            label = { Text1("Latitud") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = longitud.toString(),
            onValueChange = { longitud = it.toDoubleOrNull() ?: 0.0 },
            label = { Text1("Longitud") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = orden.toString(),
            onValueChange = { orden = it.toIntOrNull() ?: 0 },
            label = { Text1("Orden") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = costoAlojamiento.toString(),
            onValueChange = { costoAlojamiento = it.toDoubleOrNull() ?: 0.0 },
            label = { Text1("Costo Alojamiento") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = costoTraslado.toString(),
            onValueChange = { costoTraslado = it.toDoubleOrNull() ?: 0.0 },
            label = { Text1("Costo Traslado") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = comentario,
            onValueChange = { comentario = it },
            label = { Text1("Comentarios") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val nuevoRegistro = Registro(
                    lugar = lugar,
                    imagenReferencia = imagenReferencia,
                    latitud = latitud,
                    longitud = longitud,
                    orden = orden,
                    costoAlojamiento = costoAlojamiento,
                    costoTraslado = costoTraslado,
                    comentario = comentario
                )
                onGuardarClick(nuevoRegistro)
                onSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text1("Guardar Registro")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                appViewModel.cambiarPantalla2Formulario()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text1("Volver")
        }
    }
}

@Composable
fun DetalleDelRegistro(
    appViewModel: CameraAppViewModel,
    registro: Registro,
    onEditarClick: (Registro) -> Unit,
    onEliminarClick: (Registro) -> Unit
) {
    val contexto = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = registro.lugar,
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(15.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val painter = rememberImagePainter(
                data = registro.imagenReferencia,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.error)
                }
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp, 200.dp)
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Costo Alojamiento: ${registro.costoAlojamiento}",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Costo Traslado: ${registro.costoTraslado}",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "${registro.comentario}",
            style = TextStyle(
                fontSize = 15.sp
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AddAPhoto,
                contentDescription = "Tomar Foto",
                modifier = Modifier
                    .clickable {

                    }
                    .size(30.dp)
            )
            Spacer(modifier = Modifier.width(32.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "Editar",
                modifier = Modifier
                    .clickable {
                        onEditarClick(registro)
                    }
                    .size(30.dp)
            )
            Spacer(modifier = Modifier.width(32.dp))
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar Registro",
                modifier = Modifier
                    .clickable {
                        onEliminarClick(registro)
                    }
                    .size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(90.dp))

        AndroidView(
            factory = {
                MapView(it).also { mapView ->
                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                    Configuration.getInstance().userAgentValue = contexto.packageName

                    mapView.controller.setZoom(18.0)
                    val geoPoint = GeoPoint(registro.latitud, registro.longitud)
                    mapView.controller.animateTo(geoPoint)

                    val marcador = Marker(mapView)
                    marcador.position = geoPoint
                    marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    mapView.overlays.add(marcador)

                    mapView.setClickable(false)
                }
            },
            modifier = Modifier
                .width(230.dp)
                .height(90.dp)
                .offset(y = -45.dp, x = 71.dp)
        )

        Button(
            onClick = {
                appViewModel.cambiarPantalla2Formulario()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.KeyboardReturn,
                    contentDescription = "Agregar Lugar",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text1("Volver")
            }
        }
    }
}