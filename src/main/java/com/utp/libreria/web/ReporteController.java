package com.utp.libreria.web;

import com.utp.libreria.service.NegocioException;
import com.utp.libreria.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.function.Supplier;

/** M10 - Reportes (RF11, RF12). Solo Administrador. */
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String reportes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           Model model) {
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        LocalDate d = desde == null ? h.withDayOfMonth(1) : desde;
        try {
            model.addAttribute("resumen", servicio.resumenVentas(d, h));
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("resumen", servicio.resumenVentas(h.withDayOfMonth(1), h));
        }
        model.addAttribute("existencias", servicio.existencias());
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        return "reportes";
    }

    @GetMapping("/ventas.xlsx")
    public ResponseEntity<byte[]> ventasXlsx(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descarga("reporte-ventas.xlsx", XLSX, () -> servicio.ventasXlsx(desde, hasta));
    }

    @GetMapping("/ventas.csv")
    public ResponseEntity<byte[]> ventasCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return descarga("reporte-ventas.csv", CSV, () -> servicio.ventasCsv(desde, hasta));
    }

    @GetMapping("/existencias.xlsx")
    public ResponseEntity<byte[]> existenciasXlsx() {
        return descarga("reporte-existencias.xlsx", XLSX, servicio::existenciasXlsx);
    }

    @GetMapping("/existencias.csv")
    public ResponseEntity<byte[]> existenciasCsv() {
        return descarga("reporte-existencias.csv", CSV, servicio::existenciasCsv);
    }

    private ResponseEntity<byte[]> descarga(String nombre, MediaType tipo, Supplier<byte[]> generador) {
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(nombre).build().toString())
                    .contentType(tipo)
                    .body(generador.get());
        } catch (NegocioException e) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }
}
