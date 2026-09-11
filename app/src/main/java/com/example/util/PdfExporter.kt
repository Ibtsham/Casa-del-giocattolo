package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class per rappresentare una singola riga di presenza nel PDF.
 */
data class Presenza(val nome: String, val presente: Boolean)

/**
 * Data class per rappresentare una riga nella griglia del registro PDF multi-giorno.
 */
data class StudentGridRow(
    val name: String,
    val statuses: List<String>,
    val totalPresences: Int,
    val totalAbsences: Int
)

/**
 * Utility per generare ed esportare report di presenza in formato PDF tabellare
 * utilizzando la libreria iText 7.
 */
object PdfExporter {

    // Colori per lo stato delle presenze
    private val COLOR_PRESENTE = DeviceRgb(46, 125, 50)  // Verde scuro elegante
    private val COLOR_ASSENTE = DeviceRgb(198, 40, 40)   // Rosso scuro elegante
    private val COLOR_RITARDO = DeviceRgb(239, 108, 0)   // Arancione
    private val COLOR_USCITA = DeviceRgb(2, 136, 209)    // Blu chiaro

    /**
     * Genera un file PDF nella cache dell'app con una tabella a due colonne ("Alunno", "Stato")
     * applicando il colore verde per "Presente" e rosso per "Assente".
     *
     * @param context Il contesto dell'applicazione.
     * @param lista L'elenco degli alunni con il relativo stato di presenza.
     * @param fileName Il nome desiderato per il file PDF salvato.
     * @return Il file PDF generato o null in caso di errore.
     */
    fun exportPresenzeToPdf(
        context: Context,
        lista: List<Presenza>,
        fileName: String = "presenze.pdf"
    ): File? {
        return try {
            // Genera il file PDF temporaneo nella directory cache dell'app
            val file = File(context.cacheDir, fileName)

            val pdfWriter = PdfWriter(file)
            val pdfDoc = PdfDocument(pdfWriter)
            val document = Document(pdfDoc)

            // Titolo del documento
            val title = Paragraph("Foglio Presenze")
                .setFontSize(22f)
                .setBold()
                .setMarginBottom(8f)
            document.add(title)

            // Sottotitolo informativo con data e ora di generazione
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN)
            val subtitle = Paragraph("Generato il: ${sdf.format(Date())}")
                .setFontSize(10f)
                .setItalic()
                .setMarginBottom(16f)
            document.add(subtitle)

            // Tabella a 2 colonne: "Alunno" (75%) e "Stato" (25%)
            val columnWidths = floatArrayOf(3f, 1f)
            val table = Table(UnitValue.createPercentArray(columnWidths))
            table.useAllAvailableWidth()

            // Intestazione tabella
            table.addHeaderCell(
                Cell().add(
                    Paragraph("Alunno")
                        .setBold()
                        .setFontSize(12f)
                )
            )
            table.addHeaderCell(
                Cell().add(
                    Paragraph("Stato")
                        .setBold()
                        .setFontSize(12f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )

            // Righe della tabella
            lista.forEach { presenza ->
                // Colonna 1: Nome alunno
                table.addCell(
                    Cell().add(
                        Paragraph(presenza.nome)
                            .setFontSize(11f)
                    )
                )

                // Colonna 2: Stato ("Presente" in verde, "Assente" in rosso)
                val statusText = if (presenza.presente) "Presente" else "Assente"
                val statusColor = if (presenza.presente) COLOR_PRESENTE else COLOR_ASSENTE

                table.addCell(
                    Cell().add(
                        Paragraph(statusText)
                            .setFontSize(11f)
                            .setBold()
                            .setFontColor(statusColor)
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )
            }

            document.add(table)
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Genera un file PDF nella cache dell'app con una griglia di dati tabellare multi-giorno
     * contenente i nomi degli studenti, le colonne delle date dinamiche, il totale delle presenze
     * e il totale delle assenze.
     *
     * @param context Il contesto dell'applicazione.
     * @param dates Lista dei giorni dinamici da mostrare come colonne (es. ["01/03", "02/03"]).
     * @param rows Dati degli alunni con stato per ciascun giorno e relativi totali.
     * @param fileName Il nome desiderato per il file PDF salvato.
     * @return Il file PDF generato o null in caso di errore.
     */
    fun exportRegistroGridToPdf(
        context: Context,
        dates: List<String>,
        rows: List<StudentGridRow>,
        fileName: String = "registro_presenze.pdf"
    ): File? {
        return try {
            // Genera il file PDF temporaneo nella directory cache dell'app
            val file = File(context.cacheDir, fileName)

            val pdfWriter = PdfWriter(file)
            val pdfDoc = PdfDocument(pdfWriter)
            val document = Document(pdfDoc)

            // Titolo
            val title = Paragraph("Registro Presenze - Tabella Periodo")
                .setFontSize(20f)
                .setBold()
                .setMarginBottom(8f)
            document.add(title)

            // Informazioni e legenda
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN)
            val info = Paragraph(
                "Generato il: ${sdf.format(Date())}\n" +
                "Legenda: P = Presente | A = Assente | R = Ritardo | U = Uscita Ant. | - = Non programmato"
            )
                .setFontSize(9f)
                .setItalic()
                .setMarginBottom(14f)
            document.add(info)

            // Calcolo larghezze colonne: Alunno (3.5), ciascuna data (1.0), Tot. Presenze (1.2), Tot. Assenze (1.2)
            val colCount = 1 + dates.size + 2
            val columnWidths = FloatArray(colCount)
            columnWidths[0] = 3.5f // Nome Alunno
            for (i in dates.indices) {
                columnWidths[i + 1] = 1.0f // Colonne date dinamiche
            }
            columnWidths[colCount - 2] = 1.2f // Presenze
            columnWidths[colCount - 1] = 1.2f // Assenze

            val table = Table(UnitValue.createPercentArray(columnWidths))
            table.useAllAvailableWidth()

            // Header cells
            table.addHeaderCell(
                Cell().add(Paragraph("Alunno").setBold().setFontSize(10f))
            )
            dates.forEach { d ->
                table.addHeaderCell(
                    Cell().add(
                        Paragraph(d)
                            .setBold()
                            .setFontSize(9f)
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )
            }
            table.addHeaderCell(
                Cell().add(
                    Paragraph("Presenze")
                        .setBold()
                        .setFontSize(9f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            table.addHeaderCell(
                Cell().add(
                    Paragraph("Assenze")
                        .setBold()
                        .setFontSize(9f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )

            // Dati studenti
            rows.forEach { row ->
                // Nome studente
                table.addCell(
                    Cell().add(Paragraph(row.name).setFontSize(9f))
                )

                // Colonne per ciascuna data dinamica
                row.statuses.forEach { status ->
                    val paragraph = Paragraph(status)
                        .setFontSize(9f)
                        .setTextAlignment(TextAlignment.CENTER)

                    when (status) {
                        "P" -> paragraph.setFontColor(COLOR_PRESENTE).setBold()
                        "A" -> paragraph.setFontColor(COLOR_ASSENTE).setBold()
                        "R" -> paragraph.setFontColor(COLOR_RITARDO).setBold()
                        "U" -> paragraph.setFontColor(COLOR_USCITA).setBold()
                    }

                    table.addCell(Cell().add(paragraph))
                }

                // Totale presenze
                table.addCell(
                    Cell().add(
                        Paragraph(row.totalPresences.toString())
                            .setFontSize(9f)
                            .setBold()
                            .setFontColor(COLOR_PRESENTE)
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )

                // Totale assenze
                table.addCell(
                    Cell().add(
                        Paragraph(row.totalAbsences.toString())
                            .setFontSize(9f)
                            .setBold()
                            .setFontColor(COLOR_ASSENTE)
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                )
            }

            document.add(table)
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Condivide in modo sicuro il PDF generato tramite FileProvider.
     * Genera un URI con autorità ${context.packageName}.fileprovider e avvia
     * un Intent.ACTION_SEND con tipo "application/pdf".
     *
     * @param context Il contesto dell'applicazione.
     * @param pdfFile Il file PDF da condividere.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Condividi Documento PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
