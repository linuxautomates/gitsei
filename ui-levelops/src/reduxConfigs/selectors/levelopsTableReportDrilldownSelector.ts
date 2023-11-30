import { convertFromTableSchema, getFilteredRow } from "configuration-tables/helper";
import { get, unset } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getTableId = createParameterSelector((params: any) => params.tableId);
const getTableFilters = createParameterSelector((params: any) => params.tableFilters);

const _levelopsTableDataSelector = createSelector(restapiState, (data: any) => {
  return get(data, ["config_tables", "get"], { loading: true, error: false });
});

const _levelopsSpecificTableDataSelector = createSelector(
  _levelopsTableDataSelector,
  getTableId,
  (data: any, id: string) => {
    return get(data, [id, "data"], {});
  }
);

export const levelopsTableSchemaBasedModifiedData = createSelector(
  _levelopsSpecificTableDataSelector,
  (tableData: any) => {
    return convertFromTableSchema(tableData);
  }
);

const _levelOpsTableReportFilters = createSelector(
  levelopsTableSchemaBasedModifiedData,
  getTableFilters,
  (tableData: any, filters: any) => {
    const finalFilters: { dataIndex: string; value: any }[] = [];
    if (Object.keys(filters || {}).length) {
      const cols = tableData?.columns ?? [];
      const ouColumn = cols.find((col: any) => col.title === "ou_id");
      let hasDashboardOUFilter = false;
      Object.keys(filters || {}).forEach((filterKey: string) => {
        if (filterKey === "ou_id") {
          hasDashboardOUFilter = true;
          const ouValue = filters[filterKey];
          if (!!ouColumn && !!ouValue) {
            finalFilters.push({
              dataIndex: ouColumn.dataIndex,
              value: ouValue
            });
          }
        }
      });

      // removing conflicting ou_id filter i.e prioritizing dashboard ou filter over widget query ou_id filter.
      if (hasDashboardOUFilter) {
        unset(filters, [ouColumn?.id]);
      }

      Object.keys(filters || {}).forEach((filterKey: string) => {
        const columnData = cols.find((col: any) => col.id === filterKey);
        const value = filters[filterKey];
        if (columnData) {
          finalFilters.push({
            dataIndex: columnData.dataIndex,
            value
          });
        }
      });
    }
    return finalFilters;
  }
);

const _getLevelOpsTableRows = createSelector(levelopsTableSchemaBasedModifiedData, (data: any) => {
  return get(data, ["rows"], []);
});

export const getLevelOpsTableColumns = createSelector(levelopsTableSchemaBasedModifiedData, (data: any) => {
  return get(data, ["columns"], []);
});

export const levelopsTableReportFilteredRows = createSelector(
  _getLevelOpsTableRows,
  getLevelOpsTableColumns,
  _levelOpsTableReportFilters,
  (tableRows: Array<any>, tableColumns: Array<any>, filters: Array<any>) => {
    const { rows } = getFilteredRow(filters, tableRows, tableColumns, "equal");
    return rows;
  }
);
