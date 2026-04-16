#!/bin/sh
set -e

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${NACOS_DB_NAME}" <<-'EOSQL'
INSERT IGNORE INTO users (username, password, enabled)
VALUES ('nacos', '$2b$12$wI.KpOwZO/6FRonX0lxyLuDsYGMCBurElCCWrhwur29MxQg3kxdOK', TRUE);

INSERT IGNORE INTO roles (username, role)
VALUES ('nacos', 'ROLE_ADMIN');
EOSQL
