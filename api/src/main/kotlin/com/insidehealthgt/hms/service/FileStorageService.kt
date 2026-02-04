package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.FileStorageException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Data class for returning file content when downloading any document type.
 */
data class DocumentFileData(val fileName: String, val contentType: String, val fileSize: Long, val fileData: ByteArray)

/**
 * Type of document being stored, determines subdirectory name.
 */
enum class StorageDocumentType(val directoryName: String) {
    ID_DOCUMENT("id-documents"),
    CONSENT_DOCUMENT("consent-documents"),
}

/**
 * Service for storing and retrieving files on the local file system.
 *
 * Files are organized in patient-specific directories:
 * {base-path}/patients/{patientId}/{documentType}/{uuid}_{filename}
 */
@Service
class FileStorageService(
    @Value("\${app.file-storage.base-path}")
    private val basePath: String,
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(FileStorageService::class.java)
    private lateinit var baseDirectory: Path

    override fun run(args: ApplicationArguments) {
        baseDirectory = Paths.get(basePath).toAbsolutePath().normalize()

        try {
            Files.createDirectories(baseDirectory)
            logger.info("File storage initialized at: {}", baseDirectory)
        } catch (ex: IOException) {
            logger.error("Could not create file storage directory: {}", baseDirectory, ex)
            throw FileStorageException("Could not initialize file storage at: $baseDirectory")
        }

        // Validate directory is writable
        if (!Files.isWritable(baseDirectory)) {
            throw FileStorageException("File storage directory is not writable: $baseDirectory")
        }
    }

    /**
     * Store a file for a patient.
     *
     * @param patientId The patient ID for directory organization
     * @param documentType The type of document being stored
     * @param file The uploaded file
     * @return The relative storage path from base directory
     */
    fun storeFile(patientId: Long, documentType: StorageDocumentType, file: MultipartFile): String {
        val relativePath = Paths.get(
            "patients",
            patientId.toString(),
            documentType.directoryName,
        )
        return storeMultipartFile(relativePath, file)
    }

    /**
     * Load a file from storage.
     *
     * @param storagePath The relative path stored in the database
     * @return The file contents as ByteArray
     * @throws ResourceNotFoundException if file does not exist
     */
    fun loadFile(storagePath: String): ByteArray {
        val filePath = resolveAndValidatePath(storagePath)

        return try {
            Files.readAllBytes(filePath)
        } catch (ex: IOException) {
            logger.error("Failed to read file at {}: {}", filePath, ex.message, ex)
            throw FileStorageException("Unable to read the file. Please contact support.")
        }
    }

    /**
     * Resolve and validate a storage path, ensuring it's within the base directory and exists.
     */
    private fun resolveAndValidatePath(storagePath: String): Path {
        val filePath = baseDirectory.resolve(storagePath).normalize()

        // Security check: ensure path is within base directory
        if (!filePath.startsWith(baseDirectory)) {
            throw BadRequestException("Invalid storage path")
        }

        if (!Files.exists(filePath)) {
            logger.error("File not found on disk: {}", filePath)
            throw ResourceNotFoundException("The requested file could not be found. Please contact support.")
        }

        return filePath
    }

    /**
     * Store a file for an admission document.
     *
     * @param admissionId The admission ID for directory organization
     * @param file The uploaded file
     * @return The relative storage path from base directory
     */
    fun storeFileForAdmission(admissionId: Long, file: MultipartFile): String {
        val relativePath = Paths.get(
            "admissions",
            admissionId.toString(),
            "documents",
        )
        return storeMultipartFile(relativePath, file)
    }

    /**
     * Common file storage logic for MultipartFile uploads.
     *
     * @param directoryPath The relative directory path to store the file in
     * @param file The uploaded file
     * @return The relative storage path from base directory (including filename)
     */
    private fun storeMultipartFile(directoryPath: Path, file: MultipartFile): String {
        val sanitizedFilename = sanitizeFilename(file.originalFilename ?: "document")
        val uniqueFilename = "${UUID.randomUUID()}_$sanitizedFilename"

        val relativePath = directoryPath.resolve(uniqueFilename)
        val targetPath = baseDirectory.resolve(relativePath).normalize()

        // Security check: ensure target is still within base directory
        if (!targetPath.startsWith(baseDirectory)) {
            throw BadRequestException("Invalid filename. Please rename the file and try again.")
        }

        try {
            Files.createDirectories(targetPath.parent)

            file.inputStream.use { inputStream ->
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }

            logger.debug("File stored at: {}", relativePath)
            return relativePath.toString()
        } catch (ex: IOException) {
            logger.error("Failed to store file at {}: {}", targetPath, ex.message, ex)
            throw FileStorageException("Unable to save the file. Please contact support.")
        }
    }

    /**
     * Store a file directly with a given path (used for thumbnails).
     *
     * @param relativePath The relative path for the file
     * @param content The file content as bytes
     * @return The relative storage path from base directory
     */
    fun storeBytes(relativePath: Path, content: ByteArray): String {
        val targetPath = baseDirectory.resolve(relativePath).normalize()

        // Security check: ensure target is still within base directory
        if (!targetPath.startsWith(baseDirectory)) {
            throw BadRequestException("Invalid path")
        }

        try {
            // Create parent directories if needed
            Files.createDirectories(targetPath.parent)

            Files.write(targetPath, content)

            logger.debug("File stored at: {}", relativePath)
            return relativePath.toString()
        } catch (ex: IOException) {
            logger.error("Failed to store file at {}: {}", targetPath, ex.message, ex)
            throw FileStorageException("Unable to save the file. Please contact support.")
        }
    }

    /**
     * Get the absolute path for a relative storage path.
     * Used by ThumbnailService to read original files.
     */
    fun getAbsolutePath(storagePath: String): Path = resolveAndValidatePath(storagePath)

    /**
     * Delete a file from storage.
     *
     * Note: For soft deletes, this method is NOT called.
     * Only use for hard deletes or cleanup.
     */
    fun deleteFile(storagePath: String) {
        val filePath = baseDirectory.resolve(storagePath).normalize()

        if (!filePath.startsWith(baseDirectory)) {
            logger.warn("Attempted to delete file outside base directory: {}", storagePath)
            return
        }

        try {
            Files.deleteIfExists(filePath)
            logger.debug("File deleted: {}", storagePath)
        } catch (ex: IOException) {
            logger.error("Failed to delete file at {}: {}", filePath, ex.message, ex)
            // Don't throw - deletion failure should not break the application
        }
    }

    /**
     * Sanitize filename to prevent path traversal and invalid characters.
     */
    private fun sanitizeFilename(filename: String): String {
        // Remove path separators and null bytes
        val cleaned = filename
            .replace("\\", "")
            .replace("/", "")
            .replace("\u0000", "")
            .trim()

        // Validate not empty after cleaning
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "..") {
            return "document"
        }

        // Limit length
        return if (cleaned.length > MAX_FILENAME_LENGTH) {
            val extension = cleaned.substringAfterLast('.', "")
            val baseName = cleaned.substringBeforeLast('.', cleaned)
            val maxBaseLength = MAX_FILENAME_LENGTH - extension.length - 1
            if (extension.isNotEmpty() && maxBaseLength > 0) {
                "${baseName.take(maxBaseLength)}.$extension"
            } else {
                cleaned.take(MAX_FILENAME_LENGTH)
            }
        } else {
            cleaned
        }
    }

    companion object {
        private const val MAX_FILENAME_LENGTH = 200
    }
}
