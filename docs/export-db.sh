# 1) Appliquer les migrations sur une DB postgres locale (ou CI)
# Adapter les variables a votre setup
export PGHOST=localhost
export PGPORT=5432
export PGUSER=dossierfacile
export PGPASSWORD=your_very_secure_password
export PGDATABASE=dossierfacile

# 2) Generer le snapshot structurel
pg_dump \
  --schema-only \
  --no-owner \
  --no-privileges \
  --file sql-helper/schema-current.sql \
  "$PGDATABASE"