#!/usr/bin/env bash
# Copia diaria (RNF07). Programar con cron:  0 22 * * *  /ruta/db/respaldo.sh
# Usa DB_USER y DB_PASSWORD del entorno; guarda en ./respaldos con fecha.
set -euo pipefail
mkdir -p respaldos
MYSQL_PWD="${DB_PASSWORD:-}" mysqldump -u "${DB_USER:-libreria_app}" --single-transaction libreria_db \
  | gzip > "respaldos/libreria_$(date +%F).sql.gz"
find respaldos -name 'libreria_*.sql.gz' -mtime +30 -delete
