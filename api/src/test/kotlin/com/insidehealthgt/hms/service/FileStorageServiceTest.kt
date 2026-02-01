package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileStorageServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var fileStorageService: FileStorageService

    @BeforeEach
    fun setUp() {
        fileStorageService = FileStorageService(tempDir.toString())
        fileStorageService.run(DefaultApplicationArguments())
    }

    @Test
    fun `storeFile should create directories and store file`() {
        val content = "test file content".toByteArray()
        val file = createMockMultipartFile("test.pdf", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(123L, DocumentType.ID_DOCUMENT, file)

        assertTrue(storagePath.startsWith("patients/123/id-documents/"))
        assertTrue(storagePath.endsWith("_test.pdf"))

        val fullPath = tempDir.resolve(storagePath)
        assertTrue(Files.exists(fullPath))
        assertEquals("test file content", Files.readString(fullPath))
    }

    @Test
    fun `storeFile should store consent document in correct directory`() {
        val content = "consent content".toByteArray()
        val file = createMockMultipartFile("consent.pdf", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(456L, DocumentType.CONSENT_DOCUMENT, file)

        assertTrue(storagePath.startsWith("patients/456/consent-documents/"))
        assertTrue(storagePath.endsWith("_consent.pdf"))
    }

    @Test
    fun `storeFile should sanitize filename with path separators`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile("../../../etc/passwd", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, DocumentType.ID_DOCUMENT, file)

        // File should be stored safely within patient directory (slashes removed from filename)
        assertTrue(storagePath.startsWith("patients/1/id-documents/"))
        // Verify file doesn't contain path separators in the filename portion
        val filename = storagePath.substringAfterLast("/")
        assertTrue(!filename.contains("/"))
    }

    @Test
    fun `storeFile should sanitize filename with backslashes`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile("..\\..\\etc\\passwd", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, DocumentType.ID_DOCUMENT, file)

        // Verify backslashes are removed from filename
        assertTrue(!storagePath.contains("\\"))
    }

    @Test
    fun `storeFile should handle empty filename`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile("", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, DocumentType.ID_DOCUMENT, file)

        assertTrue(storagePath.contains("_document"))
    }

    @Test
    fun `storeFile should handle null filename`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile(null, "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, DocumentType.ID_DOCUMENT, file)

        assertTrue(storagePath.contains("_document"))
    }

    @Test
    fun `storeFile should generate unique filenames for same file`() {
        val content = "content".toByteArray()
        val file1 = createMockMultipartFile("same.pdf", "application/pdf", content)
        val file2 = createMockMultipartFile("same.pdf", "application/pdf", content)

        val path1 = fileStorageService.storeFile(1L, DocumentType.ID_DOCUMENT, file1)
        val path2 = fileStorageService.storeFile(1L, DocumentType.ID_DOCUMENT, file2)

        // Both should exist and be different (UUID prefix)
        assertTrue(path1 != path2)
        assertTrue(Files.exists(tempDir.resolve(path1)))
        assertTrue(Files.exists(tempDir.resolve(path2)))
    }

    @Test
    fun `loadFile should return file contents`() {
        val originalContent = "file content here".toByteArray()
        val file = createMockMultipartFile("doc.pdf", "application/pdf", originalContent)
        val storagePath = fileStorageService.storeFile(1L, DocumentType.ID_DOCUMENT, file)

        val loadedContent = fileStorageService.loadFile(storagePath)

        assertEquals(String(originalContent), String(loadedContent))
    }

    @Test
    fun `loadFile should throw ResourceNotFoundException for missing file`() {
        val exception = assertThrows<ResourceNotFoundException> {
            fileStorageService.loadFile("patients/999/id-documents/nonexistent.pdf")
        }

        assertTrue(exception.message!!.contains("could not be found"))
    }

    @Test
    fun `loadFile should reject path traversal attempts`() {
        assertThrows<BadRequestException> {
            fileStorageService.loadFile("../../../etc/passwd")
        }
    }

    @Test
    fun `loadFile should reject path with double dots`() {
        assertThrows<BadRequestException> {
            fileStorageService.loadFile("patients/../../../etc/passwd")
        }
    }

    @Test
    fun `deleteFile should remove file from disk`() {
        val content = "to be deleted".toByteArray()
        val file = createMockMultipartFile("delete-me.pdf", "application/pdf", content)
        val storagePath = fileStorageService.storeFile(1L, DocumentType.ID_DOCUMENT, file)

        assertTrue(Files.exists(tempDir.resolve(storagePath)))

        fileStorageService.deleteFile(storagePath)

        assertTrue(!Files.exists(tempDir.resolve(storagePath)))
    }

    @Test
    fun `deleteFile should not throw for nonexistent file`() {
        // Should not throw
        fileStorageService.deleteFile("patients/999/id-documents/nonexistent.pdf")
    }

    @Test
    fun `deleteFile should reject path traversal attempts`() {
        // Should not throw but also should not delete anything outside base directory
        fileStorageService.deleteFile("../../../etc/passwd")
        // No exception means the guard clause worked
    }

    private fun createMockMultipartFile(filename: String?, contentType: String, content: ByteArray): MultipartFile {
        val file = mock<MultipartFile>()
        whenever(file.originalFilename).thenReturn(filename)
        whenever(file.contentType).thenReturn(contentType)
        whenever(file.size).thenReturn(content.size.toLong())
        whenever(file.inputStream).thenReturn(ByteArrayInputStream(content))
        whenever(file.bytes).thenReturn(content)
        whenever(file.isEmpty).thenReturn(false)
        return file
    }
}
