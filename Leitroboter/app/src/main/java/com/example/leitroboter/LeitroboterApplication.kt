package com.example.leitroboter

import android.app.Application
import android.system.Os
import java.io.File

class LeitroboterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // libqi guarda su identificador de máquina en el almacenamiento privado de la app.
        val qiDirectory = File(filesDir, "qi")
        check(qiDirectory.isDirectory || qiDirectory.mkdirs())
        Os.setenv("QI_WRITABLE_PATH", qiDirectory.absolutePath, true)
    }
}
