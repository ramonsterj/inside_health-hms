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
import java.nio.file.Paths
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

        val storagePath = fileStorageService.storeFile(123L, StorageDocumentType.ID_DOCUMENT, file)

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

        val storagePath = fileStorageService.storeFile(456L, StorageDocumentType.CONSENT_DOCUMENT, file)

        assertTrue(storagePath.startsWith("patients/456/consent-documents/"))
        assertTrue(storagePath.endsWith("_consent.pdf"))
    }

    @Test
    fun `storeFile should sanitize filename with path separators`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile("../../../etc/passwd", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

        assertTrue(storagePath.startsWith("patients/1/id-documents/"))
        val filename = storagePath.substringAfterLast("/")
        assertTrue(!filename.contains("/"))
    }

    @Test
    fun `storeFile should sanitize filename with backslashes`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile("..\\..\\etc\\passwd", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

        assertTrue(!storagePath.contains("\\"))
    }

    @Test
    fun `storeFile should handle empty filename`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile("", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

        assertTrue(storagePath.contains("_document"))
    }

    @Test
    fun `storeFile should handle null filename`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile(null, "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

        assertTrue(storagePath.contains("_document"))
    }

    @Test
    fun `storeFile should generate unique filenames for same file`() {
        val content = "content".toByteArray()
        val file1 = createMockMultipartFile("same.pdf", "application/pdf", content)
        val file2 = createMockMultipartFile("same.pdf", "application/pdf", content)

        val path1 = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file1)
        val path2 = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file2)

        assertTrue(path1 != path2)
        assertTrue(Files.exists(tempDir.resolve(path1)))
        assertTrue(Files.exists(tempDir.resolve(path2)))
    }

    @Test
    fun `storeFile should sanitize null bytes in filename`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile("malicious\u0000.pdf", "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

        assertTrue(!storagePath.contains("\u0000"))
        assertTrue(Files.exists(tempDir.resolve(storagePath)))
    }

    @Test
    fun `storeFile should truncate filename longer than 200 characters`() {
        val longName = "a".repeat(250) + ".pdf"
        val content = "content".toByteArray()
        val file = createMockMultipartFile(longName, "application/pdf", content)

        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

        val filename = storagePath.substringAfterLast("/")
        // filename = UUID(36 chars) + "_" + sanitized(<=200 chars)
        val sanitizedPart = filename.substringAfter("_")
        assertTrue(sanitizedPart.length <= 200)
        assertTrue(sanitizedPart.endsWith(".pdf"))
        assertTrue(Files.exists(tempDir.resolve(storagePath)))
    }

    @Test
    fun `storeFileForAdmission should store in admission directory`() {
        val content = "admission doc".toByteArray()
        val file = createMockMultipartFile("report.pdf", "application/pdf", content)

        val storagePath = fileStorageService.storeFileForAdmission(99L, file)

        assertTrue(storagePath.startsWith("admissions/99/documents/"))
        assertTrue(storagePath.endsWith("_report.pdf"))
        assertTrue(Files.exists(tempDir.resolve(storagePath)))
        assertEquals("admission doc", Files.readString(tempDir.resolve(storagePath)))
    }

    @Test
    fun `storeBytes should store raw bytes at given path`() {
        val content = "raw bytes content".toByteArray()
        val relativePath = Paths.get("admissions", "1", "thumbnails", "thumb.png")

        val storagePath = fileStorageService.storeBytes(relativePath, content)

        assertEquals(relativePath.toString(), storagePath)
        val fullPath = tempDir.resolve(storagePath)
        assertTrue(Files.exists(fullPath))
        assertEquals("raw bytes content", Files.readString(fullPath))
    }

    @Test
    fun `storeBytes should reject path traversal`() {
        val content = "content".toByteArray()
        val maliciousPath = Paths.get("..", "..", "etc", "passwd")

        assertThrows<BadRequestException> {
            fileStorageService.storeBytes(maliciousPath, content)
        }
    }

    @Test
    fun `getAbsolutePath should return resolved path for existing file`() {
        val content = "content".toByteArray()
        val file = createMockMultipartFile("test.pdf", "application/pdf", content)
        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

        val absolutePath = fileStorageService.getAbsolutePath(storagePath)

        assertTrue(absolutePath.isAbsolute)
        assertTrue(Files.exists(absolutePath))
        assertTrue(absolutePath.startsWith(tempDir.toAbsolutePath().normalize()))
    }

    @Test
    fun `getAbsolutePath should throw for nonexistent file`() {
        assertThrows<ResourceNotFoundException> {
            fileStorageService.getAbsolutePath("patients/999/id-documents/nonexistent.pdf")
        }
    }

    @Test
    fun `getAbsolutePath should reject path traversal`() {
        assertThrows<BadRequestException> {
            fileStorageService.getAbsolutePath("../../etc/passwd")
        }
    }

    @Test
    fun `loadFile should return file contents`() {
        val originalContent = "file content here".toByteArray()
        val file = createMockMultipartFile("doc.pdf", "application/pdf", originalContent)
        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

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
        val storagePath = fileStorageService.storeFile(1L, StorageDocumentType.ID_DOCUMENT, file)

        assertTrue(Files.exists(tempDir.resolve(storagePath)))

        fileStorageService.deleteFile(storagePath)

        assertTrue(!Files.exists(tempDir.resolve(storagePath)))
    }

    @Test
    fun `deleteFile should not throw for nonexistent file`() {
        fileStorageService.deleteFile("patients/999/id-documents/nonexistent.pdf")
    }

    @Test
    fun `deleteFile should reject path traversal attempts`() {
        fileStorageService.deleteFile("../../../etc/passwd")
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
