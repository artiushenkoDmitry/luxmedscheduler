#!/usr/bin/env bash

OUTPUT_FILE="project_dump.txt"

# Очищаем или создаем выходной файл
> "$OUTPUT_FILE"

echo "Собираем файлы проекта в $OUTPUT_FILE..."

# Находим все текстовые файлы/исходники, исключая служебные каталоги
find . -type f \
  \( -name "*.scala" -o -name "*.java" -o -name "*.sbt" -o -name "*.conf" -o -name "*.properties" -o -name "*.xml" \) \
  ! -path "*/target/*" \
  ! -path "*/.git/*" \
  ! -path "*/.idea/*" \
  ! -path "*/.bloop/*" \
  ! -path "*/.metals/*" \
  ! -name "$OUTPUT_FILE" | sort | while read -r file; do

    echo "================================================================================" >> "$OUTPUT_FILE"
    echo "FILE: $file" >> "$OUTPUT_FILE"
    echo "================================================================================" >> "$OUTPUT_FILE"
    cat "$file" >> "$OUTPUT_FILE"
    echo -e "\n\n" >> "$OUTPUT_FILE"

done

echo "Готово! Содержимое проекта сохранено в $OUTPUT_FILE"
