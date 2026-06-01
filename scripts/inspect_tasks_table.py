import json
import sys
from datetime import date, datetime
from pathlib import Path

from sqlalchemy import text

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from db.database import engine


TABLE_SCHEMA = "public"
TABLE_NAME = "tasks"
SAMPLE_LIMIT = 5
MAX_VALUE_LENGTH = 120


def compact(value):
    if isinstance(value, (datetime, date)):
        return value.isoformat()
    if value is None:
        return None
    if isinstance(value, (dict, list)):
        text_value = json.dumps(value, ensure_ascii=True)
    else:
        text_value = str(value)
    if len(text_value) > MAX_VALUE_LENGTH:
        return text_value[:MAX_VALUE_LENGTH] + "..."
    return text_value


def main() -> None:
    with engine.connect() as connection:
        exists = connection.execute(
            text(
                """
                select exists (
                    select 1
                    from information_schema.tables
                    where table_schema = :schema
                      and table_name = :table
                )
                """
            ),
            {"schema": TABLE_SCHEMA, "table": TABLE_NAME},
        ).scalar_one()

        if not exists:
            print(f"Table not found: {TABLE_SCHEMA}.{TABLE_NAME}")
            return

        columns = connection.execute(
            text(
                """
                select
                    column_name,
                    data_type,
                    udt_name,
                    is_nullable,
                    column_default,
                    character_maximum_length,
                    datetime_precision
                from information_schema.columns
                where table_schema = :schema
                  and table_name = :table
                order by ordinal_position
                """
            ),
            {"schema": TABLE_SCHEMA, "table": TABLE_NAME},
        ).mappings().all()

        constraints = connection.execute(
            text(
                """
                select
                    tc.constraint_name,
                    tc.constraint_type,
                    string_agg(kcu.column_name, ', ' order by kcu.ordinal_position) as columns
                from information_schema.table_constraints tc
                left join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name
                 and tc.table_schema = kcu.table_schema
                 and tc.table_name = kcu.table_name
                where tc.table_schema = :schema
                  and tc.table_name = :table
                group by tc.constraint_name, tc.constraint_type
                order by tc.constraint_type, tc.constraint_name
                """
            ),
            {"schema": TABLE_SCHEMA, "table": TABLE_NAME},
        ).mappings().all()

        row_count = connection.execute(
            text(f"select count(*) from {TABLE_SCHEMA}.{TABLE_NAME}")
        ).scalar_one()

        sample_rows = connection.execute(
            text(f"select * from {TABLE_SCHEMA}.{TABLE_NAME} order by id limit :limit"),
            {"limit": SAMPLE_LIMIT},
        ).mappings().all()

    print(f"Table: {TABLE_SCHEMA}.{TABLE_NAME}")
    print("")

    print("Columns:")
    for column in columns:
        length = column["character_maximum_length"]
        type_name = column["data_type"]
        if length:
            type_name = f"{type_name}({length})"
        print(
            f"- {column['column_name']}: {type_name} "
            f"nullable={column['is_nullable']} "
            f"default={column['column_default']}"
        )

    print("")
    print("Constraints:")
    if constraints:
        for constraint in constraints:
            print(
                f"- {constraint['constraint_type']}: "
                f"{constraint['constraint_name']} ({constraint['columns']})"
            )
    else:
        print("- none")

    print("")
    print(f"Row count: {row_count}")

    print("")
    print(f"Sample rows, limited to {SAMPLE_LIMIT}:")
    if not sample_rows:
        print("- none")
        return

    for row in sample_rows:
        compact_row = {key: compact(value) for key, value in row.items()}
        print(json.dumps(compact_row, ensure_ascii=True, sort_keys=True))


if __name__ == "__main__":
    main()
