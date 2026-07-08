package dev.lovelace.citovision.infrastructure.persistence.preferences

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Ruta del fichero de preferencias en iOS: directorio de documentos de la app. */
fun dataStorePath(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path) + "/$PREFERENCES_FILE_NAME"
}
