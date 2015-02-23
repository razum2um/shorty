if [ "$1" != 'auto' ]; then
  read -p "pg database (empty for 'shorty'): " PG_DB
fi
PG_DB=${PG_DB:-shorty}

if [ "$1" != 'auto' ]; then
  read -p "pg test database suffix (empty for '_test'): " PG_DB_SUFF
fi
PG_DB_SUFF=${PG_DB_SUFF:-_test}

if [ "$1" != 'auto' ]; then
  read -p "pg user (empty for ${USER}): " PG_USER
fi
PG_USER=${PG_USER:-${USER}}

if [ "$1" != 'auto' ]; then
  read -p "pg user password (empty for ''):" PG_PASS
fi
PG_PASS=${PG_PASS:-''}

export PG_PASSWORD=$PG_PASS

createdb $PG_DB -O $PG_USER
createdb "${PG_DB}${PG_DB_SUFF}" -O $PG_USER

SQL='CREATE TABLE urls (id SERIAL NOT NULL, url TEXT NOT NULL, code TEXT, open_count INTEGER DEFAULT 0, CONSTRAINT urls_pkey PRIMARY KEY (id)); ALTER SEQUENCE urls_id_seq RESTART WITH 5000;CREATE UNIQUE INDEX urls_code_idx ON urls (code);'
echo $SQL | psql $PG_DB -U $PG_USER
echo $SQL | psql "${PG_DB}${PG_DB_SUFF}" -U $PG_USER

echo "{:dev-overrides {:env {:db \"${PG_DB}\" :user \"${PG_USER}\" :password \"${PG_PASS}\"}} :test-overrides {:env {:db \"${PG_DB}${PG_DB_SUFF}\" :user \"${PG_USER}\" :password \"${PG_PASS}\"}}}" > profiles.clj

