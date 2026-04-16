#!/bin/sh
set -e

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${NACOS_DB_NAME}" < /docker-entrypoint-initdb.d/20-nacos-schema.sql.txt
