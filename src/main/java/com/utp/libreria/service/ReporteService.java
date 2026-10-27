package com.utp.libreria.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.utp.libreria.model.Producto;
import com.utp.libreria.model.Venta;
import com.utp.libreria.repository.DetalleVentaRepository;
import com.utp.libreria.repository.ProductoRepository;
import com.utp.libreria.repository.VentaRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** M10 - Reportes (RF11, RF12, CU-13): consulta y exportación a Excel (Apache POI) y CSV. */
@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final ImmutableList<String> COLUMNAS_VENTAS =
            ImmutableList.of("Comprobante", "Fecha", "Cliente", "Vendedor", "Total (S/)");
    private static final ImmutableList<String> COLUMNAS_EXISTENCIAS =
            ImmutableList.of("Código", "Título", "Categoría", "Precio (S/)", "Stock", "Stock mínimo", "Estado");

    public record ProductoVendido(String codigo, String titulo, long unidades, BigDecimal monto) { }

    public record ResumenVentas(List<Venta> ventas, BigDecimal total, List<ProductoVendido> masVendidos) { }

    private final VentaRepository ventas;
    private final DetalleVentaRepository detalles;
    private final ProductoRepository productos;

    public ReporteService(VentaRepository ventas, DetalleVentaRepository detalles, ProductoRepository productos) {
        this.ventas = ventas;
        this.detalles = detalles;
        this.productos = productos;
    }

    /** RF12: totales iguales a la suma de las ventas confirmadas del rango (TC17). */
    public ResumenVentas resumenVentas(LocalDate desde, LocalDate hasta) {
        Preconditions.checkNotNull(desde, "desde");
        Preconditions.checkNotNull(hasta, "hasta");
        if (hasta.isBefore(desde)) {
            throw new NegocioException("La fecha final no puede ser anterior a la inicial.");
        }
        var inicio = desde.atStartOfDay();
        var fin = hasta.plusDays(1).atStartOfDay();
        List<Venta> lista = ventas.buscar(inicio, fin, "", "");
        BigDecimal total = lista.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ProductoVendido> top = new ArrayList<>();
        for (Object[] fila : detalles.masVendidos(inicio, fin)) {
            top.add(new ProductoVendido((String) fila[0], (String) fila[1],
                    ((Number) fila[2]).longValue(), new BigDecimal(fila[3].toString())));
        }
        return new ResumenVentas(lista, total, top);
    }

    public List<Producto> existencias() {
        return productos.findAllByOrderByTitulo();
    }

    // ---------------------------------------------------------------- Excel (Apache POI)

    public byte[] ventasXlsx(LocalDate desde, LocalDate hasta) {
        ResumenVentas r = resumenVentas(desde, hasta);
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Ventas");
            escribirFila(hoja, 0, libro, COLUMNAS_VENTAS, true);
            int fila = 1;
            for (Venta v : r.ventas()) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(neutralizar(v.getNroComprobante()));
                row.createCell(1).setCellValue(v.getFecha().format(FECHA_HORA));
                row.createCell(2).setCellValue(neutralizar(v.getCliente() == null ? "Cliente general" : v.getCliente().getNombre()));
                row.createCell(3).setCellValue(neutralizar(v.getUsuario().getNombre()));
                row.createCell(4).setCellValue(v.getTotal().doubleValue());
            }
            Row pie = hoja.createRow(fila);
            pie.createCell(3).setCellValue("TOTAL");
            pie.createCell(4).setCellValue(r.total().doubleValue());
            anchos(hoja, COLUMNAS_VENTAS.size());
            libro.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] existenciasXlsx() {
        List<Producto> lista = existencias();
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Existencias");
            escribirFila(hoja, 0, libro, COLUMNAS_EXISTENCIAS, true);
            int fila = 1;
            for (Producto p : lista) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(neutralizar(p.getCodigo()));
                row.createCell(1).setCellValue(neutralizar(p.getTitulo()));
                row.createCell(2).setCellValue(neutralizar(p.getCategoria().getNombre()));
                row.createCell(3).setCellValue(p.getPrecio().doubleValue());
                row.createCell(4).setCellValue(p.getStock());
                row.createCell(5).setCellValue(p.getStockMinimo());
                row.createCell(6).setCellValue(estado(p));
            }
            anchos(hoja, COLUMNAS_EXISTENCIAS.size());
            libro.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------------------------------------------------------------- CSV

    public byte[] ventasCsv(LocalDate desde, LocalDate hasta) {
        ResumenVentas r = resumenVentas(desde, hasta);
        StringBuilder sb = new StringBuilder("\uFEFF");   // BOM para que Excel lea las tildes
        sb.append(csv(COLUMNAS_VENTAS)).append("\r\n");
        for (Venta v : r.ventas()) {
            sb.append(csv(List.of(v.getNroComprobante(), v.getFecha().format(FECHA_HORA),
                    v.getCliente() == null ? "Cliente general" : v.getCliente().getNombre(),
                    v.getUsuario().getNombre(), v.getTotal().toPlainString()))).append("\r\n");
        }
        sb.append(csv(List.of("", "", "", "TOTAL", r.total().toPlainString()))).append("\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] existenciasCsv() {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(csv(COLUMNAS_EXISTENCIAS)).append("\r\n");
        for (Producto p : existencias()) {
            sb.append(csv(List.of(p.getCodigo(), p.getTitulo(), p.getCategoria().getNombre(),
                    p.getPrecio().toPlainString(), String.valueOf(p.getStock()),
                    String.valueOf(p.getStockMinimo()), estado(p)))).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- utilidades

    private static String estado(Producto p) {
        if (!p.isActivo()) return "Inactivo";
        return p.isBajoMinimo() ? "Bajo mínimo" : "Normal";
    }

    /**
     * Seguridad: una celda de texto que empieza con = + - @ se interpretaría como fórmula en Excel
     * (inyección de fórmulas). Se antepone una comilla simple para neutralizarla.
     */
    public static String neutralizar(String texto) {
        if (texto == null) return "";
        return StringUtils.startsWithAny(texto, "=", "+", "-", "@", "\t", "\r") ? "'" + texto : texto;
    }

    private static String csv(List<String> campos) {
        List<String> salida = new ArrayList<>();
        for (String c : campos) {
            String t = neutralizar(c);
            salida.add("\"" + t.replace("\"", "\"\"") + "\"");
        }
        return String.join(",", salida);
    }

    private static void escribirFila(Sheet hoja, int indice, Workbook libro, List<String> valores, boolean negrita) {
        Row row = hoja.createRow(indice);
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(negrita);
        estilo.setFont(fuente);
        for (int i = 0; i < valores.size(); i++) {
            Cell c = row.createCell(i);
            c.setCellValue(valores.get(i));
            c.setCellStyle(estilo);
        }
    }

    /** Ancho fijo: autoSizeColumn requiere fuentes gráficas y falla en servidores sin ellas. */
    private static void anchos(Sheet hoja, int columnas) {
        for (int i = 0; i < columnas; i++) {
            hoja.setColumnWidth(i, 22 * 256);
        }
    }
}
