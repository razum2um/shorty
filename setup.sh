echo "pg database (empty for 'shorty'):"
read PG_DB
PG_DB=${PG_DB:-shorty}

echo "pg user (empty for ${USER}):"
read PG_USER
PG_USER=${PG_USER:-${USER}}

echo "pg user password (empty for ''):"
read PG_PASS
PG_PASS=${PG_PASS:-''}

export PG_PASSWORD=$PG_PASS

createdb $PG_DB -O $PG_USER
echo 'CREATE TABLE urls (id SERIAL NOT NULL, url TEXT, hits INTEGER DEFAULT 0, CONSTRAINT urls_pkey PRIMARY KEY (id)); ALTER SEQUENCE urls_id_seq RESTART WITH 5000;' | psql $PG_DB -U $PG_USER

echo "{:dev-overrides {:env {:db \"${PG_DB}\" :user \"${PG_USER}\" :password \"${PG_PASS}\" :port 8080}}}" > profiles.clj

