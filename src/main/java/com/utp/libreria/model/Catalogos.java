package com.utp.libreria.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/** Catálogos fijos con colecciones inmutables de Guava: no pueden modificarse por error. */
public final class Catalogos {

    public static final ImmutableList<String> ROLES =
            ImmutableList.of(Rol.ADMINISTRADOR, Rol.VENDEDOR, Rol.INVENTARIO);

    public static final ImmutableMap<EstadoOrden, String> ETIQUETA_ESTADO_ORDEN = ImmutableMap.of(
            EstadoOrden.PENDIENTE, "Pendiente",
            EstadoOrden.RECIBIDA, "Recibida");

    public static final ImmutableMap<TipoMovimiento, String> ETIQUETA_MOVIMIENTO = ImmutableMap.of(
            TipoMovimiento.ENTRADA, "Entrada",
            TipoMovimiento.SALIDA, "Salida");

    private Catalogos() { }
}
