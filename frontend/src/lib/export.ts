type CsvCell = boolean | number | string | null | undefined

interface CsvColumn<T> {
  header: string
  value: (row: T, index: number) => CsvCell
}

export const todayFileToken = () => {
  const date = new Date()
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('')
}

export const downloadCsv = <T>(filename: string, rows: T[], columns: CsvColumn<T>[]) => {
  const lines = [
    columns.map((column) => escapeCsvCell(column.header)).join(','),
    ...rows.map((row, index) =>
      columns.map((column) => escapeCsvCell(column.value(row, index))).join(','),
    ),
  ]

  const blob = new Blob(['\uFEFF', lines.join('\r\n')], {
    type: 'text/csv;charset=utf-8;',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 0)
}

const escapeCsvCell = (value: CsvCell) => {
  const text = value == null ? '' : String(value)

  if (!/[",\r\n]/.test(text)) {
    return text
  }

  return `"${text.replace(/"/g, '""')}"`
}
