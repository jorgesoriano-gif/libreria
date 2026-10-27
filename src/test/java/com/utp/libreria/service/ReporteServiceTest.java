package com.utp.libreria.service;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** TC17 y RNF: reportes y exportación a Excel/CSV con neutralización de fórmulas. */
@SpringBootTest
class ReporteServiceTest {

    @Autowired ReporteService reportes;

    @Test
    void neutralizaCeldasQueSePodrianInterpretarComoFormulas() {
        assertEquals("'=SUM(A1)", ReporteService.neutralizar("=SUM(A1)"));
        assertEquals("'+cmd", ReporteService.neutralizar("+cmd"));
        assertEquals("'@x", ReporteService.neutralizar("@x"));
        assertEquals("Normal", ReporteService.neutralizar("Normal"));
        assertEquals("", ReporteService.neutralizar(null));
    }

    @Test
    void exportaExistenciasAExcelLegible() throws Exception {
        byte[] bytes = reportes.existenciasXlsx();
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet hoja = libro.getSheet("Existencias");
            assertNotNull(hoja);
            assertEquals("Código", hoja.getRow(0).getCell(0).getStringCellValue());
            assertTrue(hoja.getLastRowNum() >= 1);   // hay productos de ejemplo
        }
    }

    @Test
    void exportaVentasAExcelYCsv() throws Exception {
        LocalDate hoy = LocalDate.now();
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(reportes.ventasXlsx(hoy.minusDays(30), hoy)))) {
            assertEquals("Comprobante", libro.getSheet("Ventas").getRow(0).getCell(0).getStringCellValue());
        }
        String csv = new String(reportes.ventasCsv(hoy.minusDays(30), hoy), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"Comprobante\""));
        assertTrue(csv.contains("TOTAL"));
    }

    @Test
    void totalDelResumenEsLaSumaDeLasVentas() {
        LocalDate hoy = LocalDate.now();
        var r = reportes.resumenVentas(hoy.minusDays(365), hoy);
        var suma = r.ventas().stream().map(v -> v.getTotal()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        assertEquals(0, suma.compareTo(r.total()));
    }

    @Test
    void rechazaUnRangoInvertido() {
        LocalDate hoy = LocalDate.now();
        assertThrows(NegocioException.class, () -> reportes.resumenVentas(hoy, hoy.minusDays(1)));
    }
}
