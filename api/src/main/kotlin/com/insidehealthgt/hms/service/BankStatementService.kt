package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.SaveColumnMappingRequest
import com.insidehealthgt.hms.dto.response.BankStatementResponse
import com.insidehealthgt.hms.dto.response.BankStatementRowResponse
import com.insidehealthgt.hms.dto.response.ColumnMappingResponse
import com.insidehealthgt.hms.entity.BankAccountColumnMapping
import com.insidehealthgt.hms.entity.BankStatement
import com.insidehealthgt.hms.entity.BankStatementRow
import com.insidehealthgt.hms.entity.BankStatementStatus
import com.insidehealthgt.hms.entity.MatchStatus
import com.insidehealthgt.hms.entity.StatementFileType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.BankAccountColumnMappingRepository
import com.insidehealthgt.hms.repository.BankStatementRepository
import com.insidehealthgt.hms.repository.BankStatementRowRepository
import com.opencsv.CSVReaderBuilder
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("TooManyFunctions")
@Service
class BankStatementService(
    private val bankAccountService: BankAccountService,
    private val columnMappingRepository: BankAccountColumnMappingRepository,
    private val bankStatementRepository: BankStatementRepository,
    private val bankStatementRowRepository: BankStatementRowRepository,
    private val fileStorageService: FileStorageService,
    private val reconciliationService: ReconciliationService,
) {

    private val logger = LoggerFactory.getLogger(BankStatementService::class.java)

    @Transactional
    fun saveColumnMapping(bankAccountId: Long, request: SaveColumnMappingRequest): ColumnMappingResponse {
        val bankAccount = bankAccountService.findEntityById(bankAccountId)
        val existing = columnMappingRepository.findByBankAccountId(bankAccountId)

        val mapping = existing ?: BankAccountColumnMapping(
            bankAccount = bankAccount,
            dateColumn = request.dateColumn,
            debitColumn = request.debitColumn,
            creditColumn = request.creditColumn,
        )

        mapping.fileType = request.fileType
        mapping.hasHeader = request.hasHeader
        mapping.dateColumn = request.dateColumn
        mapping.descriptionColumn = request.descriptionColumn?.takeIf { it.isNotBlank() }
        mapping.referenceColumn = request.referenceColumn?.takeIf { it.isNotBlank() }
        mapping.debitColumn = request.debitColumn
        mapping.creditColumn = request.creditColumn
        mapping.balanceColumn = request.balanceColumn?.takeIf { it.isNotBlank() }
        mapping.dateFormat = request.dateFormat
        mapping.skipRows = request.skipRows

        val saved = columnMappingRepository.save(mapping)
        return ColumnMappingResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun getColumnMapping(bankAccountId: Long): ColumnMappingResponse? {
        bankAccountService.findEntityById(bankAccountId)
        val mapping = columnMappingRepository.findByBankAccountId(bankAccountId) ?: return null
        return ColumnMappingResponse.from(mapping)
    }

    @Suppress("ThrowsCount")
    @Transactional
    fun uploadStatement(bankAccountId: Long, file: MultipartFile, statementDate: LocalDate): BankStatementResponse {
        val bankAccount = bankAccountService.findEntityById(bankAccountId)
        if (bankAccount.isPettyCash) {
            throw BadRequestException("Bank statements cannot be uploaded for petty cash accounts")
        }

        val mapping = columnMappingRepository.findByBankAccountId(bankAccountId)
            ?: throw BadRequestException("Column mapping must be configured before uploading statements")

        val fileName = file.originalFilename ?: "statement"
        val expectedType = mapping.fileType
        val actualExtension = fileName.substringAfterLast('.', "").uppercase()
        if (expectedType == StatementFileType.XLSX && actualExtension != "XLSX") {
            throw BadRequestException("Expected XLSX file but received .$actualExtension")
        }
        if (expectedType == StatementFileType.CSV && actualExtension != "CSV") {
            throw BadRequestException("Expected CSV file but received .$actualExtension")
        }

        val fileBytes = file.bytes

        val storagePath = fileStorageService.storeBankStatement(bankAccountId, file)

        val statement = BankStatement(
            bankAccount = bankAccount,
            fileName = fileName,
            filePath = storagePath,
            statementDate = statementDate,
        )
        val savedStatement = bankStatementRepository.save(statement)

        val parsedRows = when (mapping.fileType) {
            StatementFileType.XLSX -> parseXlsx(fileBytes, mapping)
            StatementFileType.CSV -> parseCsv(fileBytes, mapping)
        }

        var rowNumber = 1
        val rows = parsedRows.map { parsed ->
            val row = BankStatementRow(
                bankStatement = savedStatement,
                rowNumber = rowNumber++,
                transactionDate = parsed.date,
                description = parsed.description,
                reference = parsed.reference,
                debitAmount = parsed.debit,
                creditAmount = parsed.credit,
                balance = parsed.balance,
            )
            bankStatementRowRepository.save(row)
        }

        savedStatement.totalRows = rows.size
        savedStatement.unmatchedCount = rows.size
        bankStatementRepository.save(savedStatement)

        reconciliationService.autoMatch(savedStatement.id!!)

        val refreshed = bankStatementRepository.findById(savedStatement.id!!).get()
        return BankStatementResponse.from(refreshed)
    }

    @Transactional(readOnly = true)
    fun getStatements(bankAccountId: Long): List<BankStatementResponse> {
        bankAccountService.findEntityById(bankAccountId)
        return bankStatementRepository.findAllByBankAccountIdOrderByStatementDateDesc(bankAccountId)
            .map { BankStatementResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getStatement(bankAccountId: Long, statementId: Long): BankStatementResponse {
        val statement = findStatementEntity(bankAccountId, statementId)
        return BankStatementResponse.from(statement)
    }

    @Transactional(readOnly = true)
    fun getStatementRows(bankAccountId: Long, statementId: Long): List<BankStatementRowResponse> {
        findStatementEntity(bankAccountId, statementId)
        val rows = bankStatementRowRepository.findAllByBankStatementIdOrderByRowNumberAsc(statementId)
        return rows.map { row ->
            val description = reconciliationService.resolveMatchedEntityDescription(row)
            BankStatementRowResponse.from(row, description)
        }
    }

    @Transactional
    fun deleteStatement(bankAccountId: Long, statementId: Long) {
        val statement = findStatementEntity(bankAccountId, statementId)
        val rows = bankStatementRowRepository.findAllByBankStatementIdOrderByRowNumberAsc(statementId)
        val now = LocalDateTime.now()
        rows.forEach { it.deletedAt = now }
        bankStatementRowRepository.saveAll(rows)
        statement.deletedAt = now
        bankStatementRepository.save(statement)
    }

    @Transactional
    fun completeStatement(bankAccountId: Long, statementId: Long): BankStatementResponse {
        val statement = findStatementEntity(bankAccountId, statementId)
        val unmatchedCount = bankStatementRowRepository.countByBankStatementIdAndMatchStatus(
            statementId,
            MatchStatus.UNMATCHED,
        )
        val suggestedCount = bankStatementRowRepository.countByBankStatementIdAndMatchStatus(
            statementId,
            MatchStatus.SUGGESTED,
        )
        if (unmatchedCount > 0 || suggestedCount > 0) {
            throw BadRequestException(
                "Cannot complete statement: $unmatchedCount unmatched and $suggestedCount suggested rows remain",
            )
        }
        statement.status = BankStatementStatus.COMPLETED
        val saved = bankStatementRepository.save(statement)
        return BankStatementResponse.from(saved)
    }

    fun findStatementEntity(bankAccountId: Long, statementId: Long): BankStatement =
        bankStatementRepository.findByIdAndBankAccountId(statementId, bankAccountId)
            ?: throw ResourceNotFoundException("Bank statement not found with id: $statementId")

    @Suppress("TooGenericExceptionCaught")
    private fun parseXlsx(fileBytes: ByteArray, mapping: BankAccountColumnMapping): List<ParsedRow> {
        val rows = mutableListOf<ParsedRow>()
        val dateFormatter = DateTimeFormatter.ofPattern(mapping.dateFormat)

        ByteArrayInputStream(fileBytes).use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            var startRow = mapping.skipRows
            if (mapping.hasHeader) startRow++

            val headerRow = if (mapping.hasHeader) sheet.getRow(mapping.skipRows) else null
            val columnIndexMap = if (headerRow != null) {
                buildColumnIndexMap(headerRow, mapping)
            } else {
                buildNumericColumnIndexMap(mapping)
            }

            for (i in startRow..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                try {
                    val date = getCellDate(row, columnIndexMap["date"]!!, dateFormatter)
                    val debit = getCellDecimal(row, columnIndexMap["debit"])
                    val credit = getCellDecimal(row, columnIndexMap["credit"])

                    if (date != null && (debit != null || credit != null)) {
                        rows.add(
                            ParsedRow(
                                date = date,
                                description = getCellString(row, columnIndexMap["description"]),
                                reference = getCellString(row, columnIndexMap["reference"]),
                                debit = debit,
                                credit = credit,
                                balance = getCellDecimal(row, columnIndexMap["balance"]),
                            ),
                        )
                    }
                } catch (ex: Exception) {
                    logger.warn("Skipping row {} due to parse error: {}", i, ex.message)
                }
            }
            workbook.close()
        }
        return rows
    }

    @Suppress("TooGenericExceptionCaught", "CyclomaticComplexMethod")
    private fun parseCsv(fileBytes: ByteArray, mapping: BankAccountColumnMapping): List<ParsedRow> {
        val rows = mutableListOf<ParsedRow>()
        val dateFormatter = DateTimeFormatter.ofPattern(mapping.dateFormat)

        ByteArrayInputStream(fileBytes).use { inputStream ->
            val reader = CSVReaderBuilder(InputStreamReader(inputStream))
                .withSkipLines(mapping.skipRows)
                .build()

            val allLines = reader.readAll()
            val headerLine = if (mapping.hasHeader && allLines.isNotEmpty()) allLines[0] else null
            val dataStartIdx = if (mapping.hasHeader) 1 else 0

            val columnIndexMap = if (headerLine != null) {
                buildCsvColumnIndexMap(headerLine, mapping)
            } else {
                buildNumericColumnIndexMap(mapping)
            }

            for (i in dataStartIdx until allLines.size) {
                val line = allLines[i]
                try {
                    val dateStr = line.getOrNull(columnIndexMap["date"] ?: -1)?.trim()
                    val date = if (!dateStr.isNullOrBlank()) {
                        LocalDate.parse(dateStr, dateFormatter)
                    } else {
                        null
                    }

                    val debit = parseCsvDecimal(line.getOrNull(columnIndexMap["debit"] ?: -1))
                    val credit = parseCsvDecimal(line.getOrNull(columnIndexMap["credit"] ?: -1))

                    if (date != null && (debit != null || credit != null)) {
                        rows.add(
                            ParsedRow(
                                date = date,
                                description = line.getOrNull(columnIndexMap["description"] ?: -1)?.trim()
                                    ?.takeIf { it.isNotBlank() },
                                reference = line.getOrNull(columnIndexMap["reference"] ?: -1)?.trim()
                                    ?.takeIf { it.isNotBlank() },
                                debit = debit,
                                credit = credit,
                                balance = parseCsvDecimal(line.getOrNull(columnIndexMap["balance"] ?: -1)),
                            ),
                        )
                    }
                } catch (ex: Exception) {
                    logger.warn("Skipping CSV row {} due to parse error: {}", i, ex.message)
                }
            }
            reader.close()
        }
        return rows
    }

    private fun buildColumnIndexMap(
        headerRow: org.apache.poi.ss.usermodel.Row,
        mapping: BankAccountColumnMapping,
    ): Map<String, Int> {
        val headers = mutableMapOf<String, Int>()
        for (cell in headerRow) {
            val name = cell.stringCellValue?.trim()?.lowercase()
            if (name != null) {
                headers[name] = cell.columnIndex
            }
        }
        return resolveColumnIndices(headers, mapping)
    }

    private fun buildCsvColumnIndexMap(
        headerLine: Array<String>,
        mapping: BankAccountColumnMapping,
    ): Map<String, Int> {
        val headers = headerLine.mapIndexed { idx, h -> h.trim().lowercase() to idx }.toMap()
        return resolveColumnIndices(headers, mapping)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun resolveColumnIndices(headers: Map<String, Int>, mapping: BankAccountColumnMapping): Map<String, Int> {
        fun resolve(column: String?, defaultIdx: Int): Int =
            column?.let { headers[it.lowercase()] ?: it.toIntOrNull() } ?: defaultIdx

        return mapOf(
            "date" to resolve(mapping.dateColumn, 0),
            "description" to resolve(mapping.descriptionColumn, -1),
            "reference" to resolve(mapping.referenceColumn, -1),
            "debit" to resolve(mapping.debitColumn, 1),
            "credit" to resolve(mapping.creditColumn, 2),
            "balance" to resolve(mapping.balanceColumn, -1),
        )
    }

    private fun buildNumericColumnIndexMap(mapping: BankAccountColumnMapping): Map<String, Int> = mapOf(
        "date" to (mapping.dateColumn.toIntOrNull() ?: 0),
        "description" to (mapping.descriptionColumn?.toIntOrNull() ?: -1),
        "reference" to (mapping.referenceColumn?.toIntOrNull() ?: -1),
        "debit" to (mapping.debitColumn.toIntOrNull() ?: 1),
        "credit" to (mapping.creditColumn.toIntOrNull() ?: 2),
        "balance" to (mapping.balanceColumn?.toIntOrNull() ?: -1),
    )

    @Suppress("ReturnCount")
    private fun getCellDate(
        row: org.apache.poi.ss.usermodel.Row,
        colIdx: Int,
        dateFormatter: DateTimeFormatter,
    ): LocalDate? {
        val cell = row.getCell(colIdx) ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
                cell.localDateTimeCellValue.toLocalDate()
            } else {
                null
            }

            CellType.STRING -> {
                val str = cell.stringCellValue?.trim() ?: return null
                if (str.isBlank()) return null
                LocalDate.parse(str, dateFormatter)
            }

            else -> null
        }
    }

    @Suppress("ReturnCount")
    private fun getCellDecimal(row: org.apache.poi.ss.usermodel.Row, colIdx: Int?): BigDecimal? {
        if (colIdx == null || colIdx < 0) return null
        val cell = row.getCell(colIdx) ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> {
                val value = BigDecimal.valueOf(cell.numericCellValue)
                if (value.compareTo(BigDecimal.ZERO) == 0) null else value.setScale(2, RoundingMode.HALF_UP)
            }

            CellType.STRING -> parseCsvDecimal(cell.stringCellValue)

            else -> null
        }
    }

    @Suppress("ReturnCount")
    private fun getCellString(row: org.apache.poi.ss.usermodel.Row, colIdx: Int?): String? {
        if (colIdx == null || colIdx < 0) return null
        val cell = row.getCell(colIdx) ?: return null
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue?.trim()?.takeIf { it.isNotBlank() }
            CellType.NUMERIC -> cell.numericCellValue.toString()
            else -> null
        }
    }

    private fun parseCsvDecimal(value: String?): BigDecimal? {
        if (value.isNullOrBlank()) return null
        val cleaned = value.trim().replace(",", "").replace(" ", "")
        return try {
            val decimal = BigDecimal(cleaned)
            if (decimal.compareTo(BigDecimal.ZERO) == 0) {
                null
            } else {
                decimal.setScale(2, RoundingMode.HALF_UP)
            }
        } catch (@Suppress("SwallowedException") ex: NumberFormatException) {
            null
        }
    }

    private data class ParsedRow(
        val date: LocalDate,
        val description: String?,
        val reference: String?,
        val debit: BigDecimal?,
        val credit: BigDecimal?,
        val balance: BigDecimal?,
    )
}
