/**
 * Pluma y Papel - Sistema de Búsqueda Automática en Tiempo Real
 * Aplica a todos los módulos del sistema y a la búsqueda global del encabezado.
 */
(function() {
    'use strict';

    // Normalizar texto: minúsculas, sin tildes ni acentos para búsqueda insensible
    function normalizar(texto) {
        if (!texto) return '';
        return texto.toString().toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .trim();
    }

    // =========================================================================
    // 1. BÚSQUEDA AUTOMÁTICA EN TABLAS (En todas las páginas del sistema)
    // =========================================================================
    function inicializarBusquedaTablas() {
        // Encontrar tablas de datos
        const tablas = document.querySelectorAll('.data-table');
        if (!tablas.length) return;

        tablas.forEach(tabla => {
            const tbody = tabla.querySelector('tbody');
            if (!tbody) return;

            // Encontrar el contenedor o formulario de filtros cercano
            const page = tabla.closest('#page') || document.body;
            const filterForm = page.querySelector('.filters-bar') || page.querySelector('form[method="get"]');
            if (!filterForm) return;

            // Inputs de búsqueda de texto
            const textInputs = filterForm.querySelectorAll('input[type="text"], input[name="q"], input[type="search"], #filtroQ, #q, #nro, #cliente, #filtroCategorias, #filtroUsuarios, #filtroCompras');
            // Selects de filtro (categoría, estado, proveedor)
            const selects = filterForm.querySelectorAll('select');
            // Elemento de conteo
            const countContainer = page.querySelector('.table-records-count');

            // Fila de mensaje "sin resultados"
            let filaVacia = tbody.querySelector('.fila-sin-resultados');
            if (!filaVacia) {
                filaVacia = document.createElement('tr');
                filaVacia.className = 'fila-sin-resultados';
                filaVacia.style.display = 'none';
                const colCount = tabla.querySelectorAll('thead th').length || 7;
                filaVacia.innerHTML = `<td colspan="${colCount}" style="text-align:center; padding: 26px 16px; color: var(--text-muted); font-size: 0.92rem;">
                    🔍 No se encontraron registros que coincidan con la búsqueda.
                </td>`;
                tbody.appendChild(filaVacia);
            }

            // Filas de datos originales (excluyendo mensajes de vacío originales de thymeleaf)
            function obtenerFilasDatos() {
                return Array.from(tbody.querySelectorAll('tr')).filter(tr => 
                    !tr.classList.contains('fila-sin-resultados') && 
                    !tr.querySelector('td[colspan]')
                );
            }

            let filasOriginales = obtenerFilasDatos();

            // Función principal de filtrado en tiempo real (0ms de retraso)
            function filtrarTablaEnVivo() {
                // Obtener texto de los inputs
                let queryText = '';
                textInputs.forEach(inp => {
                    if (inp.value) queryText += ' ' + inp.value;
                });
                queryText = normalizar(queryText);
                const tokens = queryText.split(/\s+/).filter(t => t.length > 0);

                // Obtener selects
                const selectCat = filterForm.querySelector('#filtroCat, select[name="categoria"]');
                const catVal = selectCat ? selectCat.value : '0';

                const selectEstado = filterForm.querySelector('#estado, select[name="estado"]');
                const estadoVal = selectEstado ? selectEstado.value.trim().toUpperCase() : '';

                const selectProveedor = filterForm.querySelector('#proveedor, select[name="proveedor"]');
                const provVal = selectProveedor ? selectProveedor.value : '0';

                let filas = obtenerFilasDatos();
                let visibles = 0;

                filas.forEach(tr => {
                    const textoFila = normalizar(
                        tr.textContent + ' ' +
                        (tr.getAttribute('data-codigo') || '') + ' ' +
                        (tr.getAttribute('data-titulo') || '') + ' ' +
                        (tr.getAttribute('data-autor') || '')
                    );

                    // 1. Filtro de Categoría
                    if (catVal && catVal !== '0') {
                        const rowCat = tr.getAttribute('data-categoria');
                        if (rowCat) {
                            if (rowCat !== catVal) {
                                tr.style.display = 'none';
                                return;
                            }
                        } else if (selectCat && selectCat.selectedIndex > 0) {
                            const catText = normalizar(selectCat.options[selectCat.selectedIndex]?.text || '');
                            if (catText && !textoFila.includes(catText)) {
                                tr.style.display = 'none';
                                return;
                            }
                        }
                    }

                    // 2. Filtro de Estado
                    if (estadoVal) {
                        const estadoText = normalizar(selectEstado.options[selectEstado.selectedIndex]?.text || '');
                        if (!textoFila.includes(estadoText) && !textoFila.includes(normalizar(estadoVal))) {
                            tr.style.display = 'none';
                            return;
                        }
                    }

                    // 3. Filtro de Proveedor
                    if (provVal && provVal !== '0') {
                        const rowProv = tr.getAttribute('data-proveedor');
                        if (rowProv) {
                            if (rowProv !== provVal) {
                                tr.style.display = 'none';
                                return;
                            }
                        } else if (selectProveedor && selectProveedor.selectedIndex > 0) {
                            const provText = normalizar(selectProveedor.options[selectProveedor.selectedIndex]?.text || '');
                            if (provText && !textoFila.includes(provText)) {
                                tr.style.display = 'none';
                                return;
                            }
                        }
                    }

                    // 4. Filtro de texto (todos los tokens deben coincidir en la fila o sus atributos data)
                    if (tokens.length > 0) {
                        const coincide = tokens.every(tok => textoFila.includes(tok));
                        if (!coincide) {
                            tr.style.display = 'none';
                            return;
                        }
                    }

                    // Si pasó todas las condiciones, mostrar
                    tr.style.display = '';
                    visibles++;
                });

                // Mostrar/ocultar fila de sin resultados
                if (visibles === 0) {
                    filaVacia.style.display = '';
                } else {
                    filaVacia.style.display = 'none';
                }

                // Actualizar contador de registros
                if (countContainer) {
                    const spanNum = countContainer.querySelector('span');
                    if (spanNum) {
                        spanNum.textContent = visibles;
                    } else {
                        countContainer.textContent = `Mostrando ${visibles} registros...`;
                    }
                }
            }

            // Temporizador para sincronización con el servidor (debounce)
            let debounceSyncTimer = null;
            function sincronizarConServidor() {
                if (filterForm.tagName === 'FORM' && (filterForm.method || '').toLowerCase() === 'get' && filterForm.action) {
                    clearTimeout(debounceSyncTimer);
                    debounceSyncTimer = setTimeout(() => {
                        const formData = new FormData(filterForm);
                        const params = new URLSearchParams();
                        for (let [key, val] of formData.entries()) {
                            if (val) params.append(key, val);
                        }
                        const newUrl = filterForm.action.split('?')[0] + (params.toString() ? '?' + params.toString() : '');
                        
                        // Actualizar la URL en la barra del navegador sin recargar la página
                        window.history.replaceState(null, '', newUrl);

                        // Fetch en segundo plano para sincronizar
                        fetch(newUrl, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                            .then(res => {
                                if (!res.ok) throw new Error('Error al sincronizar');
                                return res.text();
                            })
                            .then(html => {
                                const parser = new DOMParser();
                                const doc = parser.parseFromString(html, 'text/html');
                                const nuevoTbody = doc.querySelector('.data-table tbody');
                                if (nuevoTbody && nuevoTbody.innerHTML) {
                                    tbody.innerHTML = nuevoTbody.innerHTML;
                                    tbody.appendChild(filaVacia);
                                    filasOriginales = obtenerFilasDatos();
                                    filtrarTablaEnVivo();
                                }
                            })
                            .catch(() => {
                                // Fallback silencioso: el filtrado local inmediato ya está activo
                            });
                    }, 350);
                }
            }

            // Escuchar eventos en tiempo real en los inputs de texto
            textInputs.forEach(input => {
                // Envolver en contenedor con botón de limpiar si no está envuelto
                if (!input.parentElement.classList.contains('search-input-wrapper')) {
                    const wrapper = document.createElement('div');
                    wrapper.className = 'search-input-wrapper';
                    input.parentNode.insertBefore(wrapper, input);
                    wrapper.appendChild(input);

                    const clearBtn = document.createElement('span');
                    clearBtn.className = 'clear-icon';
                    clearBtn.innerHTML = '✕';
                    clearBtn.title = 'Limpiar búsqueda';
                    clearBtn.style.display = input.value ? 'flex' : 'none';
                    wrapper.appendChild(clearBtn);

                    clearBtn.addEventListener('click', () => {
                        input.value = '';
                        clearBtn.style.display = 'none';
                        input.focus();
                        filtrarTablaEnVivo();
                        sincronizarConServidor();
                    });

                    input.addEventListener('input', () => {
                        clearBtn.style.display = input.value ? 'flex' : 'none';
                    });
                }

                // Filtrar en vivo en cada tecla (evento input)
                input.addEventListener('input', () => {
                    filtrarTablaEnVivo();
                    sincronizarConServidor();
                });
            });

            // Escuchar cambios en los selects (categoría, estado, etc.)
            selects.forEach(select => {
                select.addEventListener('change', () => {
                    filtrarTablaEnVivo();
                    sincronizarConServidor();
                });
            });

            // Evitar recarga completa si el usuario presiona Enter o hace clic en "Buscar"
            filterForm.addEventListener('submit', (e) => {
                e.preventDefault();
                filtrarTablaEnVivo();
                sincronizarConServidor();
            });

            // Ejecutar filtrado inicial si ya había valores cargados en el input
            let hayFiltroInicial = false;
            textInputs.forEach(inp => { if (inp.value) hayFiltroInicial = true; });
            if (hayFiltroInicial) {
                filtrarTablaEnVivo();
            }
        });
    }

    // =========================================================================
    // 2. BÚSQUEDA GLOBAL EN LA BARRA SUPERIOR (HEADER)
    // =========================================================================
    function inicializarBusquedaGlobal() {
        const globalInput = document.getElementById('globalSearchInput');
        const resultsDropdown = document.getElementById('globalSearchResults');
        const clearBtn = document.getElementById('globalSearchClear');
        if (!globalInput || !resultsDropdown) return;

        let debounceTimer = null;
        let selectedIndex = -1;

        // Atajo de teclado global: Ctrl + K o barra diagonal '/'
        document.addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
                e.preventDefault();
                globalInput.focus();
                globalInput.select();
            } else if (e.key === '/' && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
                e.preventDefault();
                globalInput.focus();
                globalInput.select();
            }
        });

        // Evento input para buscar en tiempo real
        globalInput.addEventListener('input', () => {
            const query = globalInput.value.trim();
            if (clearBtn) clearBtn.style.display = query ? 'inline-block' : 'none';

            if (!query) {
                resultsDropdown.style.display = 'none';
                resultsDropdown.innerHTML = '';
                return;
            }

            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                fetch(`/api/busqueda-global?q=${encodeURIComponent(query)}`)
                    .then(res => res.json())
                    .then(items => {
                        renderizarResultadosGlobales(items, query);
                    })
                    .catch(() => {
                        resultsDropdown.innerHTML = `<div class="search-empty">Error al consultar el servidor.</div>`;
                        resultsDropdown.style.display = 'block';
                    });
            }, 200);
        });

        if (clearBtn) {
            clearBtn.addEventListener('click', () => {
                globalInput.value = '';
                clearBtn.style.display = 'none';
                resultsDropdown.style.display = 'none';
                globalInput.focus();
            });
        }

        // Navegación con teclado en el dropdown
        globalInput.addEventListener('keydown', (e) => {
            const items = resultsDropdown.querySelectorAll('.search-item');
            if (resultsDropdown.style.display === 'none' || !items.length) {
                if (e.key === 'Enter') {
                    // Si presiona enter sin dropdown, redirige a productos con esa búsqueda
                    const q = globalInput.value.trim();
                    if (q) window.location.href = `/productos?q=${encodeURIComponent(q)}`;
                }
                return;
            }

            if (e.key === 'ArrowDown') {
                e.preventDefault();
                selectedIndex = (selectedIndex + 1) % items.length;
                actualizarSeleccion(items);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                selectedIndex = (selectedIndex - 1 + items.length) % items.length;
                actualizarSeleccion(items);
            } else if (e.key === 'Enter') {
                e.preventDefault();
                if (selectedIndex >= 0 && selectedIndex < items.length) {
                    items[selectedIndex].click();
                } else if (items.length > 0) {
                    items[0].click();
                }
            } else if (e.key === 'Escape') {
                resultsDropdown.style.display = 'none';
            }
        });

        function actualizarSeleccion(items) {
            items.forEach((item, idx) => {
                item.classList.toggle('active', idx === selectedIndex);
                if (idx === selectedIndex) {
                    item.scrollIntoView({ block: 'nearest' });
                }
            });
        }

        function renderizarResultadosGlobales(items, query) {
            selectedIndex = -1;
            if (!items || items.length === 0) {
                resultsDropdown.innerHTML = `<div class="search-empty">No se encontraron resultados para "<strong>${escapeHtml(query)}</strong>"</div>`;
                resultsDropdown.style.display = 'block';
                return;
            }

            // Agrupar por categoría
            const grupos = {};
            items.forEach(it => {
                if (!grupos[it.categoria]) grupos[it.categoria] = [];
                grupos[it.categoria].push(it);
            });

            let html = '';
            for (let [cat, list] of Object.entries(grupos)) {
                html += `<div class="search-group-title">${cat}</div>`;
                list.forEach(it => {
                    html += `
                        <a href="${it.url}" class="search-item">
                            <div class="search-item-title">${escapeHtml(it.titulo)}</div>
                            <div class="search-item-subtitle">${escapeHtml(it.detalle)}</div>
                        </a>
                    `;
                });
            }

            resultsDropdown.innerHTML = html;
            resultsDropdown.style.display = 'block';
        }

        function escapeHtml(str) {
            if (!str) return '';
            return str.replace(/&/g, '&amp;')
                      .replace(/</g, '&lt;')
                      .replace(/>/g, '&gt;')
                      .replace(/"/g, '&quot;')
                      .replace(/'/g, '&#039;');
        }

        // Cerrar resultados al hacer clic fuera
        document.addEventListener('click', (e) => {
            if (!globalInput.contains(e.target) && !resultsDropdown.contains(e.target)) {
                resultsDropdown.style.display = 'none';
            }
        });
    }

    // Inicializar al cargar el DOM
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            inicializarBusquedaTablas();
            inicializarBusquedaGlobal();
        });
    } else {
        inicializarBusquedaTablas();
        inicializarBusquedaGlobal();
    }
})();

