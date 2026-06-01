import sys
from pathlib import Path

from sqlalchemy import text

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import engine


def main() -> None:
    with engine.connect() as connection:
        database_info = connection.execute(
            text(
                """
                select
                    current_database() as database_name,
                    current_user as user_name
                """
            )
        ).mappings().one()

        tables = connection.execute(
            text(
                """
                select table_schema, table_name
                from information_schema.tables
                where table_type = 'BASE TABLE'
                  and table_schema not in ('pg_catalog', 'information_schema')
                order by table_schema, table_name
                """
            )
        ).mappings().all()

    print(f"Connected to database: {database_info['database_name']}")
    print(f"Connected as user: {database_info['user_name']}")

    if not tables:
        print("No application tables found.")
        return

    print("Application tables:")
    for table in tables:
        print(f"- {table['table_schema']}.{table['table_name']}")


if __name__ == "__main__":
    main()
