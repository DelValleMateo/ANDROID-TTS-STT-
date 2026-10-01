package com.uader.ptah.data.tts

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Administra la localización, verificación y descompresión de los archivos requeridos
 * por el modelo neural VITS/Piper en Sherpa-ONNX (ej. vits-piper-es_AR-daniela-high).
 *
 * Estructura de archivos esperada:
 * - es_AR-daniela-high.onnx (o model.onnx)
 * - tokens.txt
 * - espeak-ng-data/ (fonemas y reglas fonéticas espeak-ng en español)
 */
object PiperModelManager {

    private const val TAG = "PiperModelManager"
    const val PIPER_DIR = "piper"
    private const val ASSET_FOLDER = "vits-piper-es_AR-daniela-high"
    private const val COMPLETE_MARKER = ".complete"

    data class ModelFiles(
        val modelPath: String,
        val tokensPath: String,
        val dataDir: String,
        val lexiconPath: String = ""
    )

    /**
     * Resuelve y prepara los archivos del modelo Piper VITS.
     *
     * 1. Verifica si ya existen en el almacenamiento interno (filesDir/piper).
     * 2. Si no están, los extrae desde assets/vits-piper-es_AR-daniela-high hacia filesDir/piper.
     * 3. Verifica en almacenamiento externo si estuvieran allí.
     *
     * @return [ModelFiles] con las rutas absolutas si todos los componentes requeridos existen, o null si faltan.
     */
    fun prepareModel(context: Context): ModelFiles? {
        val targetDir = File(context.filesDir, PIPER_DIR)

        // 1. Verificar si ya están completos en filesDir/piper
        checkExistingFiles(targetDir)?.let {
            Log.i(TAG, "Modelo Piper VITS verificado en almacenamiento interno: ${targetDir.absolutePath}")
            return it
        }

        // 2. Verificar en getExternalFilesDir(null)/piper
        context.getExternalFilesDir(null)?.let { extDir ->
            val extPiperDir = File(extDir, PIPER_DIR)
            checkExistingFiles(extPiperDir)?.let {
                Log.i(TAG, "Modelo Piper VITS encontrado en almacenamiento externo: ${extPiperDir.absolutePath}")
                return it
            }
        }

        // 3. Extraer desde assets/vits-piper-es_AR-daniela-high o assets raíz
        if (extractFromAssetsIfPresent(context, ASSET_FOLDER, targetDir) ||
            extractFromAssetsIfPresent(context, PIPER_DIR, targetDir) ||
            extractFromAssetsIfPresent(context, "", targetDir)
        ) {
            checkExistingFiles(targetDir)?.let {
                Log.i(TAG, "Modelo Piper VITS extraído exitosamente de assets a: ${targetDir.absolutePath}")
                return it
            }
        }

        Log.w(
            TAG,
            "No se encontraron los archivos del modelo Piper VITS.\n" +
                "Verifique que existan en assets/$ASSET_FOLDER/ o en ${targetDir.absolutePath}.\n" +
                "Archivos requeridos: *.onnx, tokens.txt, espeak-ng-data/"
        )
        return null
    }

    /**
     * Verifica que los archivos indispensables existan y no estén vacíos.
     */
    fun checkExistingFiles(dir: File): ModelFiles? {
        if (!dir.exists() || !dir.isDirectory) return null

        // Buscar cualquier archivo .onnx en el directorio
        val modelFile = dir.listFiles { file ->
            file.isFile && file.name.endsWith(".onnx", ignoreCase = true) && file.length() > 1000
        }?.firstOrNull() ?: return null

        val tokensFile = File(dir, "tokens.txt")
        if (!tokensFile.exists() || tokensFile.length() == 0L) return null

        val espeakDataDir = File(dir, "espeak-ng-data")
        if (!espeakDataDir.exists() || !espeakDataDir.isDirectory) return null

        val espeakChildren = espeakDataDir.list()
        if (espeakChildren.isNullOrEmpty()) return null

        val lexiconFile = File(dir, "lexicon.txt").takeIf { it.exists() }

        return ModelFiles(
            modelPath = modelFile.absolutePath,
            tokensPath = tokensFile.absolutePath,
            dataDir = espeakDataDir.absolutePath,
            lexiconPath = lexiconFile?.absolutePath ?: ""
        )
    }

    /**
     * Extrae recursivamente archivos del modelo desde assets si están presentes.
     */
    private fun extractFromAssetsIfPresent(context: Context, assetPrefix: String, targetDir: File): Boolean {
        val assetManager = context.assets
        try {
            val list = assetManager.list(assetPrefix) ?: return false
            val hasOnnx = list.any { it.endsWith(".onnx", ignoreCase = true) }
            val hasTokens = list.contains("tokens.txt")
            val hasEspeak = list.contains("espeak-ng-data")

            if (!hasOnnx || !hasTokens || !hasEspeak) {
                return false
            }

            Log.i(TAG, "Extrayendo modelo Piper VITS desde assets ('$assetPrefix') hacia ${targetDir.absolutePath}...")
            if (!targetDir.exists()) targetDir.mkdirs()

            copyAssetRecursively(assetManager, assetPrefix, targetDir)

            File(targetDir, COMPLETE_MARKER).createNewFile()
            Log.i(TAG, "Extracción de assets Piper VITS finalizada con éxito.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo modelo Piper desde assets", e)
            return false
        }
    }

    private fun copyAssetRecursively(assets: AssetManager, assetPath: String, destDir: File) {
        val list = assets.list(assetPath) ?: return

        for (child in list) {
            val childAssetPath = if (assetPath.isEmpty()) child else "$assetPath/$child"
            val childDest = File(destDir, child)

            var isFile = false
            try {
                assets.open(childAssetPath).use { input ->
                    isFile = true
                    if (!childDest.exists() || childDest.length() == 0L) {
                        childDest.parentFile?.mkdirs()
                        val tempFile = File(childDest.parentFile, "${childDest.name}.tmp")
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output, bufferSize = 128 * 1024)
                        }
                        if (tempFile.renameTo(childDest)) {
                            Log.d(TAG, "Copiado asset: $childAssetPath -> ${childDest.name} (${childDest.length()} bytes)")
                        } else {
                            tempFile.copyTo(childDest, overwrite = true)
                            tempFile.delete()
                        }
                    }
                }
            } catch (_: Exception) {
                isFile = false
            }

            if (!isFile) {
                childDest.mkdirs()
                copyAssetRecursively(assets, childAssetPath, childDest)
            }
        }
    }
}
