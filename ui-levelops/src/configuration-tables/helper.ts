import { forEach, get, uniq } from "lodash";
import moment from "moment";
import { isvalidTimeStamp, valueToUtcUnixTime } from "utils/dateUtils";
import { RestConfigTable } from "../classes/RestConfigTable";

export const PAGE_SIZE = 25;
export const PAGE_SIZE_ARR: number[] = [];
for (let x = 0; x <= PAGE_SIZE; x++) {
  PAGE_SIZE_ARR.push(x);
}

export const getNewData = (data: Array<any>, newEle: any, pos: number): Array<any> => {
  if (pos === -1) {
    return [newEle, ...data];
  } else {
    return [...data.slice(0, pos + 1), newEle, ...data.slice(pos + 1, data.length + 1)];
  }
};

export const adjustRowId = (rowId: string, tablePage: number, filteredMap: { [key: number]: number } | undefined) => {
  let adjusted_rowId = rowId;
  let rowIndex = parseInt(getRowIndex(rowId));
  // The edge case is: when this action is triggered
  // by a click on the first dot before any row on the
  // current page of the table.
  const thatWeirdEdgeCase = (rowIndex + 1) % PAGE_SIZE === 0 && (rowIndex + 1) / PAGE_SIZE + 1 === tablePage;
  const calculate_initial_index = (tablePage - 1) * PAGE_SIZE > rowIndex && !thatWeirdEdgeCase;
  let newRowIndex: number = rowIndex;

  if (calculate_initial_index) {
    newRowIndex = rowIndex + (tablePage - 1) * PAGE_SIZE;
  }

  if (!isNaN(rowIndex) && filteredMap) {
    let mappedRowIndex = filteredMap[newRowIndex];

    newRowIndex = mappedRowIndex === undefined ? newRowIndex : mappedRowIndex;
  }

  if (calculate_initial_index || (!isNaN(rowIndex) && filteredMap)) {
    //@ts-ignore
    adjusted_rowId = ["row", newRowIndex];
    if (getColIndex(rowId) !== undefined) {
      //@ts-ignore
      adjusted_rowId.push(getColIndex(rowId));
    }
    //@ts-ignore
    adjusted_rowId = adjusted_rowId.join("_");
  }

  return adjusted_rowId;
};

const getRowIndex = (rowId: string) => {
  return rowId.split("_")[1];
};

export const getColIndex = (rowId: string): string | undefined => {
  return rowId.split("_")[2];
};

const getDataIndex = (data: Array<any>, pos: number): number => {
  const col = data[pos];
  return parseInt(col.dataIndex.split("_")[1]);
};

export const getNewColDataIndex = (data: Array<any>, pos: number): string => {
  let prev = 0;
  let next = getDataIndex(data, 0);
  if (pos === data.length - 1) {
    prev = getDataIndex(data, pos);
    next = prev + 200;
  } else if (pos !== -1) {
    prev = getDataIndex(data, pos);
    next = getDataIndex(data, pos + 1);
  }

  const newIndex = Math.round((prev + next) / 2);

  return `column_${newIndex}`;
};

export const getUpdatedColumns = (columns: Array<any>, newCol: any, pos: number) => {
  let updatedColumns: Array<any> = [];
  let updatedArray: Array<any>;

  if (pos === -1) {
    updatedColumns = [newCol];
    updatedArray = columns;
  } else {
    const oldColId = `column_${pos}`;
    const realColIndex = columns.findIndex(col => col.dataIndex === oldColId);
    if (realColIndex === undefined) {
      // This would be a strange error.
      return columns;
    }
    const splitIndex = realColIndex + 1;
    updatedColumns = [...columns.slice(0, splitIndex), newCol];
    updatedArray = columns.slice(splitIndex, columns.length + 1);
  }

  updatedArray.forEach(col => {
    const prevDataIndex = parseInt(col.dataIndex.split("_")[1]);
    const newPos = prevDataIndex + 1;
    updatedColumns = [
      ...updatedColumns,
      {
        ...col,
        dataIndex: `column_${newPos}`,
        key: `column_${newPos}`,
        title: col.title || `Column ${newPos}`,
        index: newPos
      }
    ];
  });

  return updatedColumns;
};

export const getUpdatedRows = (rows: Array<any>, newColIndex: string) => {
  const newCol = parseInt(newColIndex.split("_")[1]);
  return rows.map(row => {
    const noChangeKeys = Object.keys(row).filter(
      rowKey => rowKey.includes("column") && parseInt(rowKey.split("_")[1]) < newCol
    );
    const changeKeys = Object.keys(row).filter(
      rowKey => rowKey.includes("column") && parseInt(rowKey.split("_")[1]) >= newCol
    );
    let newKeys = {
      [newColIndex]: "",
      id: row.id,
      index: row.index
    };
    noChangeKeys.forEach(nKey => {
      newKeys = {
        ...newKeys,
        [nKey]: row[nKey]
      };
    });
    changeKeys.forEach(cKey => {
      const prevKey = parseInt(cKey.split("_")[1]);
      const newKey = `column_${prevKey + 1}`;
      newKeys = {
        ...newKeys,
        [newKey]: row[cKey]
      };
    });
    return newKeys;
  });
};

export const getDefaultInputValue = (inputType: string) => {
  switch (inputType) {
    case "string":
      return "text (DEFAULT)";
    case "boolean":
      return "false (DEFAULT)";
    case "date":
      return `${moment().format("MM/DD/YYYY")} (DEFAULT)`;
    case "single-select":
      return "preset (DEFAULT)";
    default:
      return "";
  }
};

export const getColData = (data: Array<any>, colIndex: number) => {
  const column = data.find((col: any, index: number) => index === colIndex);
  return column ? { dataIndex: column.dataIndex, inputType: column.inputType } : undefined;
};

export const getDateValue = (val: any) => {
  let value = val;
  value = typeof value === "string" ? value : typeof value === "number" ? value.toString() : moment().unix().toString();
  if (isvalidTimeStamp(value)) {
    return moment.unix(parseInt(value)).format("MM/DD/YYYY");
  }
  return value;
};

export const validateCell = (inputType: string, value: any, options?: Array<string>): boolean => {
  switch (inputType) {
    case "string":
      return typeof value === "string" && value.length > 0;
    case "boolean":
      return typeof value === "string" && (value === "False" || value === "True");
    case "date":
      return moment(getDateValue(value)).isValid();
    case "single-select": {
      return typeof value === "string" && value.length > 0 && (options || []).length > 0;
    }
    default:
      return false;
  }
};

export const validateTableCells = (columns: Array<any>, rows: Array<any>) => {
  let newData: { inputType: string; options: string[]; value: any }[] = [];
  columns.forEach(col => {
    rows.forEach(row => {
      if (col.required) {
        newData.push({
          inputType: col.inputType,
          options: col.options || [],
          value: row[col.dataIndex]
        });
      }
    });
  });

  return newData.reduce((acc, item) => acc && validateCell(item.inputType, item.value, item.options), true);
};

export const defaultTableColumns = [
  {
    title: "Column 1",
    dataIndex: "column_1",
    key: "column_1",
    inputType: "string",
    editable: true
  },
  {
    title: "Column 2",
    dataIndex: "column_2",
    key: "column_2",
    inputType: "boolean",
    editable: true
  },
  {
    title: "Column 3",
    dataIndex: "column_3",
    inputType: "date",
    key: "column_3",
    editable: true
  },
  {
    title: "Column 4",
    dataIndex: "column_4",
    inputType: "single-select",
    key: "column_4",
    editable: true,
    options: ["preset"]
  }
];

export const defaultRowData = [
  {
    column_1: "",
    column_2: "",
    column_3: "",
    column_4: ""
  }
];

export const convertArrayToObject = (data: Array<any>, key: string = "id") => {
  return data.reduce((obj, item) => {
    return {
      ...obj,
      [item[key]]: item
    };
  }, {});
};

export const convertToTableSchema = (data: any) => {
  const rows = get(data, ["rows"], []);
  const columns = get(data, ["columns"], []);
  if (rows.length > 0 && columns.length > 0) {
    const newTable = new RestConfigTable(data);

    newTable.convertToTableColAndRows(rows, columns);

    return newTable;
  }
  return undefined;
};

export const convertFromTableSchema = (data: any) => {
  let tableData = data;
  const rows = get(data, ["rows"], {});
  const columns = get(data, ["schema", "columns"], {});

  if (Object.keys(rows).length > 0 && Object.keys(columns).length > 0) {
    const table = new RestConfigTable(data);

    return table.convertFromTable();
  }
  if (!Object.keys(rows).length) {
    tableData.rows = [];
  }
  if (!Object.keys(columns).length) {
    tableData.columns = [];
  }
  return tableData;
};

export const checkForColumnName = (columns: Array<any>, colId: string, value: string) => {
  return (
    columns.filter((item: any) => item.dataIndex !== colId && item.title.toLowerCase() === value.toLowerCase()).length >
    0
  );
};

export const deleteEmptyRows = (table: any) => {
  const rows = get(table, ["rows"], []);
  table.rows = rows.filter((row: any) =>
    Object.keys(row || {}).reduce((acc: boolean, obj: any) => {
      acc = acc || (row[obj] !== "" && row[obj] !== null && row[obj]);
      return acc;
    }, false)
  );
  return table;
};

export const mapColumnsWithRows = (rows: Array<any>, columns: Array<any>) => {
  return columns.map((column: any, colIndex) => {
    let values: { [x: string]: any } = {};
    rows.forEach((row: any, rowIndex) => {
      Object.keys(row).forEach(key => {
        const val = row[key];
        if (key === column.dataIndex && val.length > 0) {
          values[`row_${row.id}_${column.dataIndex}`] = val;
        }
      });
    }, {});
    return {
      title: column.title,
      dataIndex: column.dataIndex,
      values
    };
  });
};

export const mapColumnsWithRowsById = (rows: Array<any>, columns: Array<any>) => {
  return columns.map((column: any, colIndex) => {
    let values: { [x: string]: any } = {};
    rows.forEach((row: any, rowIndex) => {
      Object.keys(row).forEach(key => {
        const val = row[key];
        if (key === column.dataIndex && val.length > 0) {
          values[`row_${rowIndex}_${column.id}`] = val;
        }
      });
    }, {});
    return {
      title: column.title,
      id: column.id,
      type: column.inputType,
      values
    };
  });
};

export const getFilteredRow = (filtersData: Array<any>, rows: Array<any>, columns: Array<any>, searchType?: string) => {
  let indexes = rows.map((item, index) => index);

  filtersData.forEach((filter: any) => {
    // The values from the applied filters
    let filterValue = filter.value;

    if (!!filterValue) {
      indexes = indexes.filter((index: number) => {
        let rowData = rows[index][filter.dataIndex] || "";

        // To make filtering correct, whitespaces before and after needs to be removed
        rowData.trim();

        const columnName = columns.find((col: any) => col.dataIndex === filter.dataIndex);
        if (
          columnName?.inputType === "date" &&
          filterValue.hasOwnProperty("$lt") &&
          filterValue.hasOwnProperty("$gt")
        ) {
          const gt = parseInt(filterValue?.$gt);
          const lt = parseInt(filterValue?.$lt);
          const rowTimestamp = valueToUtcUnixTime(rowData);
          return rowTimestamp && rowTimestamp >= gt && rowTimestamp <= lt;
        }

        if (typeof filterValue === "string") {
          filterValue = filterValue
            ?.split(",")
            .map((_filter: string) => _filter.trim())
            .filter((_filter: string) => !!_filter);
        }

        if (filterValue && Array.isArray(filterValue)) {
          for (let _filter of filterValue) {
            _filter = _filter.toLowerCase();
            if (searchType === "contains" && rowData.toLowerCase().includes(_filter)) {
              return true;
            }
            if (searchType === "equal" && rowData.toLowerCase() === _filter?.toString().toLowerCase()) {
              return true;
            }
          }
          return false;
        }

        return true;
      });
    }
  });
  const filterMap = {};
  const _rows = indexes.map((index: number, i: number) => {
    //@ts-ignore
    filterMap[i] = index;
    return rows[index];
  });

  return { rows: _rows, filterMap };
};

export const validateHeaderNames = (columns: Array<any>) => {
  return uniq(columns.map(column => column.title)).length === columns.length;
};

export const validateHeaderCell = (columns: Array<any>, colId: string, value: any) => {
  return columns.filter((col: any) => col.dataIndex !== colId && col.title === value).length === 0;
};

// Let's say you're on the last page - page 5, for example -
// and change filters so that there are only 4 pages of
// results. Unfortunately, we need to manually check and
// adjust the page number in this case.
export const fixTablePageEdgeCase = (currentTablePage: number, totalItems: number) => {
  let truePageNumber = currentTablePage;
  const trueLastPageNumber = totalItems ? Math.ceil(totalItems / PAGE_SIZE) : 1;
  if (currentTablePage > trueLastPageNumber) {
    truePageNumber = trueLastPageNumber;
  }

  return { truePageNumber, wasFixed: truePageNumber !== currentTablePage };
};
