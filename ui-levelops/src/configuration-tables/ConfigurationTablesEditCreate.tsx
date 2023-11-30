import { Form, Input, notification, Spin } from "antd";
// @ts-ignore
import FileSaver from "file-saver";
import { debounce, get } from "lodash";
import queryString from "query-string";
import React, { useCallback, useEffect, useState } from "react";
import { connect } from "react-redux";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { v1 as uuid } from "uuid";
import { checkTemplateNameExists } from "../configurations/helpers/checkTemplateNameExits";
import { NAME_EXISTS_ERROR, REQUIRED_FIELD } from "../constants/formWarnings";
import { CONFIG_TABLE_ROUTES, getBaseUrl } from "../constants/routePaths";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import {
  configTableCreateState,
  configTableUpdateState,
  configTableViewState
} from "reduxConfigs/selectors/restapiSelector";
import { AntCol, AntModal, AntRow } from "../shared-resources/components";
import { getData, loadingStatus } from "../utils/loadingUtils";
import ActionComponent from "./ActionComponent";
import "./ConfigurationTablesConfigureModal.scss";
import ConfigureTable, { EditableContext } from "./Configure";
import {
  adjustRowId,
  checkForColumnName,
  convertFromTableSchema,
  convertToTableSchema,
  defaultRowData,
  defaultTableColumns,
  deleteEmptyRows,
  fixTablePageEdgeCase,
  getColData,
  getColIndex,
  getDateValue,
  getFilteredRow,
  getNewData,
  getUpdatedColumns,
  getUpdatedRows,
  PAGE_SIZE,
  PAGE_SIZE_ARR,
  validateHeaderNames,
  validateTableCells
} from "./helper";
import { CSVImportModal } from "./importcsvModal/importcsvModal.component";
import { ConfigTableFilter } from "./index";
import { scrollElementById } from "utils/domUtils";
import { ANT_LAYOUT_CONTENT_ELEMENT_ID } from "constants/elementIds";
import { convertUnixToDate } from "utils/dateUtils";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

interface ConfigurationTablesEditCreateProps {
  configTable: any;
  configTablesCreateState: any;
  configTablesUpdateState: any;
  page: any;
  history: any;
  location: any;
  setPageSettings: (path: string, settings: any) => void;
  setPageButtonAction: (path: string, btnType: string, attributes: any) => void;
  setPageDropDownAction: (path: string, btnType: string, attributes: any) => void;
  clearPageSettings: (path: string) => void;
  restapiClear: (uri: string, method: string, id?: string) => void;
  configTablesGet: (tableId: string) => void;
  configTablesUpdate: (tableId: string, tableData: any) => void;
  configTablesCreate: (tableData: any) => void;
  configTablesList: (filters: any, id?: string) => void;
  rest_api: any;
}

const ConfigurationTablesEditCreate: React.FC<ConfigurationTablesEditCreateProps> = (
  props: ConfigurationTablesEditCreateProps
) => {
  const tableId = queryString.parse(props.location.search).id;
  const versionId = queryString.parse(props.location.search).version;
  const importCSV = queryString.parse(props.location.search).import === "true";
  const filterInQuery = (queryString.parse(props.location.search).filters as string) || "";
  const searchType = (queryString.parse(props.location.search).searchType as string) || "contains";
  const filtersFromURL = filterInQuery ? JSON.parse(filterInQuery) : {};
  const [tableLoading, setTableLoading] = useState<boolean>(tableId !== undefined);
  const [createTableModal, setCreateTableModal] = useState<boolean>(!tableId && !importCSV);
  const [search, setSearch] = useState(searchType);

  const [table, setTable] = useState<any>({});

  // Pages start at 1. Hard to believe, but true.
  const [tablePage, setTablePage] = useState<number>(1);
  const [tableName, setTableName] = useState<string>("");
  const [updateBtnStatus, setUpdateBtnStatus] = useState<number>(0);
  const [checkName, setCheckName] = useState<boolean>(false);
  const [nameExists, setNameExists] = useState<any>(undefined);
  const [nameFieldBlur, setNameFieldBlur] = useState<boolean>(false);
  const [showImportCSV, setShowImportCSV] = useState<boolean>(importCSV);
  const [reImporting, setReImporting] = useState<any>(undefined);

  const [rowsEdit, setRowsEdit] = useState({});
  const [edit, setEdit] = useState<boolean>(false);

  const [filtersData, setFiltersData] = useState<Array<any>>([]);

  const [selectedIndexes, setSelectedIndexes] = useState<Array<any>>([]);

  let filteredMap: { [key: number]: number };

  useEffect(() => {
    if (versionId && tableId) {
      props.configTablesGet(`${tableId}/revisions/${versionId}?expand=schema,rows,history`);
    } else if (tableId) {
      props.configTablesGet(`${tableId}?expand=schema,rows,history`);
    }
  }, []);

  useEffect(() => {
    if (versionId) {
      setTableLoading(true);
      props.configTablesGet(`${tableId}/revisions/${versionId}?expand=schema,rows,history`);
    }
  }, [versionId]);

  useEffect(() => {
    if (tableLoading) {
      const loading = get(props.configTable, ["loading"], true);
      const error = get(props.configTable, ["error"], true);

      if (!loading && !error) {
        const data = get(props.configTable, ["data"], undefined);
        if (data) {
          const tableData = convertFromTableSchema(data);
          const filters: any[] = [];
          const selectedIndexes: string[] = [];
          if (Object.keys(filtersFromURL || {})) {
            const cols = tableData.columns;
            Object.keys(filtersFromURL || {}).forEach((filterKey: string) => {
              if (filterKey === "ou_id") {
                const ouColumn = cols.find((col: any) => col.title === "ou_id");
                const ouValue = filtersFromURL[filterKey];
                if (!!ouColumn && !!ouValue) {
                  filters.push({
                    dataIndex: ouColumn.dataIndex,
                    value: ouValue
                  });
                  selectedIndexes.push(ouColumn.dataIndex);
                }
                return;
              }
              const columnData = cols.find((col: any) => col.id === filterKey);
              const value = filtersFromURL[filterKey];
              if (columnData) {
                filters.push({
                  dataIndex: columnData.dataIndex,
                  value
                });
                selectedIndexes.push(columnData.dataIndex);
              }
            });
          }
          setTable(tableData);
          setTableName(data.name);
          !!filters.length && setFiltersData(filters);
          !!selectedIndexes.length && setSelectedIndexes(selectedIndexes);
        }
        setTableLoading(false);
      }
    }
  }, [props.configTable]);

  useEffect(() => {
    if (tableLoading) {
      const api = tableId ? props.configTablesUpdateState : props.configTablesCreateState;
      const loading = get(api, ["loading"], true);
      const error = get(api, ["error"], true);

      if (!loading && !error) {
        props.history.push(`${getBaseUrl()}${CONFIG_TABLE_ROUTES.LIST}`);
        setTableLoading(false);
      }
    }
  }, [props.configTablesCreateState, props.configTablesUpdateState]);

  const [hasCreateAccess, hasEditAccess] = useConfigScreenPermissions();

  useEffect(() => {
    if (!tableLoading) {
      let selectedValue;
      if (versionId) {
        selectedValue = { key: versionId, label: `version ${versionId}` };
      }
      props.setPageSettings(props.location.pathname, {
        title: table.name ? table.name : "",
        action_buttons: {
          export: {
            type: "secondary",
            label: "Export CSV",
            icon: "export",
            hasClicked: false,
            disabled: tableId ? !hasCreateAccess : !hasEditAccess
          },
          reImport: {
            type: "secondary",
            label: "Re-import CSV",
            icon: "import",
            hasClicked: false,
            disabled: tableId ? !hasCreateAccess : !hasEditAccess
          },
          settings: {
            type: "secondary",
            label: "Settings",
            icon: "setting",
            hasClicked: false,
            disabled: tableId ? !hasCreateAccess : !hasEditAccess
          },
          save: {
            type: "primary",
            label: "Save",
            icon: "save",
            hasClicked: false,
            disabled: tableId ? !hasCreateAccess : !hasEditAccess
          }
        },
        dropdown_buttons: {
          version: {
            label: "Version",
            options: Object.keys(table.history || {}).map(item => ({
              key: item,
              label: `version ${item} - ${convertUnixToDate(table?.history?.[item]?.created_at)}`
            })),
            selected_option: selectedValue
          }
        }
      });
    }
  }, [tableLoading]);

  useEffect(() => {
    const status = !(
      validateTableCells(table.columns || [], table.rows || []) && validateHeaderNames(table.columns || [])
    );
    props.setPageButtonAction(props.location.pathname, "save", { disabled: status });
    props.setPageButtonAction(props.location.pathname, "export", { disabled: status });
  }, [updateBtnStatus]);

  useEffect(() => {
    const action_buttons = get(props.page, [props.location.pathname, "action_buttons"], undefined);
    const dropdown_buttons = get(props.page, [props.location.pathname, "dropdown_buttons"], undefined);
    if (dropdown_buttons) {
      const versionClick = get(dropdown_buttons, ["version", "hasClicked"], false);
      if (versionClick) {
        const version = get(dropdown_buttons, ["version", "selected_option"], undefined);
        if (version) {
          props.history.push(`${getBaseUrl()}${CONFIG_TABLE_ROUTES.EDIT}?id=${tableId}&version=${version.key}`);
        }
        props.setPageDropDownAction(props.location.pathname, "version", { hasClicked: false });
      }
    }
    if (action_buttons) {
      const settingsBtnClick = get(action_buttons, ["settings", "hasClicked"], false);
      const saveBtnClick = get(action_buttons, ["save", "hasClicked"], false);
      const exportBtnClick = get(action_buttons, ["export", "hasClicked"], false);
      const reImportBtnClick = get(action_buttons, ["reImport", "hasClicked"], false);

      if (settingsBtnClick) {
        setCreateTableModal(true);
        props.setPageButtonAction(props.location.pathname, "settings", { hasClicked: false });
      }

      if (saveBtnClick) {
        // check for validation here, delete any empty rows
        const newTable = deleteEmptyRows(table);

        if ((newTable.rows || []).length > 0) {
          const tableData = convertToTableSchema(newTable);
          if (tableId) {
            props.configTablesUpdate(tableId as string, tableData);
          } else {
            props.configTablesCreate(tableData);
          }

          setTableLoading(true);
        } else {
          notification.error({
            message: "Check data"
          });
        }

        props.setPageButtonAction(props.location.pathname, "save", { hasClicked: false });
      }

      if (exportBtnClick) {
        const _header = table.columns.map((col: any) => `"${col.title}"`).join(",");
        const _rowdata = table.rows.map((row: any) => {
          const keys = Object.keys(row).filter(key => key.includes("column_"));
          const lowerbound = Math.min(...keys.map(k => +k.split("_")[1]));
          const uperbound = Math.max(...keys.map(k => +k.split("_")[1]));
          const r = [];
          for (let i = lowerbound; i <= uperbound; i++) {
            const _columnKey = `column_${i}`;
            if (keys.includes(_columnKey)) {
              const _column = (table?.columns ?? []).find((col: any) => col.dataIndex === _columnKey);
              let _val = row[_columnKey]?.replace(/"/g, '""');
              if (_column && _column.inputType === "date") {
                _val = getDateValue(_val);
              }
              r.push(`"${_val}"`);
            }
          }
          return r;
        });
        const csvData = [_header, ..._rowdata].join("\n");
        let fileName = `${table.name}_${table.version}`;
        if (!table.version) {
          fileName = "new_file";
        }
        let file = new File([csvData], `${fileName}.csv`, { type: "text/csv" });
        FileSaver.saveAs(file);
        props.setPageButtonAction(props.location.pathname, "export", { hasClicked: false });
      }
      if (reImportBtnClick) {
        props.setPageButtonAction(props.location.pathname, "reImport", { hasClicked: false });
        setReImporting(true);
        setShowImportCSV(true);
      }
    }
  }, [props.page]);

  useEffect(() => {
    if (checkName) {
      const { loading, error } = loadingStatus(props.rest_api, "config_tables", "list");
      if (!loading && !error) {
        const data = getData(props.rest_api, "config_tables", "list").records;
        setNameExists(checkTemplateNameExists(tableName, data));
        setCheckName(false);
      }
    }
  });

  useEffect(() => {
    return () => {
      setTableName("");
      props.clearPageSettings(props.location.pathname);
      props.restapiClear("config_tables", "list", "0");
      props.restapiClear("config_tables", "get", "-1");
      props.restapiClear("config_tables", "update", "-1");
      props.restapiClear("config_tables", "create", "0");
    };
  }, []);

  const setHeader = (name: string) => {
    const { page, location, setPageSettings } = props;
    let _page = page[location.pathname];
    if (_page && _page.hasOwnProperty("title")) {
      setPageSettings(location.pathname, {
        ..._page,
        title: name
      });
    }
  };

  const checkTemplateName = (name: string) => {
    const filters = {
      filter: {
        partial: {
          name
        }
      }
    };
    props.configTablesList(filters);
    setCheckName(true);
  };

  const debounceCheckName = debounce(checkTemplateName, 300);

  const updateTableName = (name: string) => {
    const columns = get(table, ["columns", "length"], 0);
    const rows = get(table, ["rows", "length"], 0);

    let tableData = table;
    tableData = {
      ...(tableData || {}),
      name
    };

    if (columns === 0) {
      tableData = {
        ...(tableData || {}),
        columns: defaultTableColumns.map(column => {
          return {
            id: uuid(),
            ...column
          };
        })
      };
    }

    if (rows === 0) {
      tableData = {
        ...(tableData || {}),
        rows: defaultRowData.map(row => {
          return {
            id: uuid(),
            ...row
          };
        })
      };
    }

    setTable(tableData);
    setHeader(name);
    setCreateTableModal(false);
    setTableName("");
    setNameFieldBlur(false);
    setUpdateBtnStatus(updateBtnStatus + 1);
  };

  const getValidateStatus = () => {
    if (!nameFieldBlur) {
      return "";
    } else if (nameFieldBlur && tableName.length > 0 && !nameExists) {
      return "success";
    } else return "error";
  };

  const getError = () => {
    if (nameExists === true) {
      return NAME_EXISTS_ERROR;
    } else return REQUIRED_FIELD;
  };

  const createModal = () => {
    let name = "";

    if (table.name) {
      name = table.name;
    }

    return (
      <AntModal
        title={table?.name ? "Table Settings" : "Create Table"}
        mask={true}
        maskClosable={false}
        visible={createTableModal}
        onOk={() => updateTableName(tableName)}
        okButtonProps={{ disabled: tableName.length === 0 }}
        onCancel={() => {
          setCreateTableModal(false);
          setTableName("");
          setNameFieldBlur(false);
          if (Object.keys(table).length === 0) {
            props.history.push(`${getBaseUrl()}${CONFIG_TABLE_ROUTES.LIST}`);
          }
        }}
        okText={table?.name ? "Update" : "Create"}
        closable={false}>
        <AntRow gutter={[16, 16]}>
          <AntCol span={24}>
            <Form layout="vertical">
              <Form.Item
                label="Name"
                validateStatus={getValidateStatus()}
                required
                hasFeedback={true}
                help={getValidateStatus() === "error" && getError()}>
                <Input name={"Name"} onFocus={() => setNameFieldBlur(true)} onChange={setName} defaultValue={name} />
              </Form.Item>
            </Form>
          </AntCol>
        </AntRow>
      </AntModal>
    );
  };

  const handleImport = (data: { name: string; columns: any[]; rows: any[] }) => {
    setShowImportCSV(false);
    const updatedName = reImporting ? table.name : data.name || table.name;
    setTable((table: any) => ({
      ...table,
      ...data,
      name: updatedName
    }));
    setHeader(updatedName);
    setNameFieldBlur(false);
    setReImporting(false);
    setUpdateBtnStatus((s: number) => s + 1);
  };

  const rowEdit = (id: string | number, edit: boolean) => {
    let editRows = rowsEdit;

    Object.keys(editRows).forEach(row => {
      if (row !== id) {
        editRows = {
          ...editRows,
          [row]: false
        };
      }
    });

    setRowsEdit({ ...editRows, [id]: edit });
  };

  const addRow = (index: string) => {
    const adjustedIndex = adjustRowId(index, tablePage, filteredMap);
    const rowIndex = adjustedIndex.split("_")[1];
    const pos = parseInt(rowIndex);
    let newRow = {};
    (table.columns || []).forEach((col: any) => {
      const key = col.dataIndex;
      const colToCopy = filtersData.find((item: any) => item.dataIndex === key);
      newRow = {
        ...newRow,
        [key]: colToCopy?.value || col.defaultValue || ""
      };
    });

    newRow = {
      id: uuid(),
      ...newRow
    };

    const updatedRows = getNewData(table.rows || [], newRow, pos);

    if ((pos + 1) / PAGE_SIZE === tablePage) {
      setTablePage(oldPage => oldPage + 1);
      scrollElementById(ANT_LAYOUT_CONTENT_ELEMENT_ID, { top: 0, left: 0, behavior: "smooth" });
    }
    setTable({ ...table, rows: updatedRows });
    setUpdateBtnStatus(updateBtnStatus + 1);
  };

  const onFiltersChange = (filter: any) => {
    if (search === "equal") {
      setSearch("contains");
    }
    let newFilters: any[] = [];
    const _filterExists = filtersData.findIndex(data => data.dataIndex === filter.dataIndex);

    if (_filterExists === -1) {
      newFilters = [...filtersData, filter];
    } else {
      newFilters = filtersData.map(data => {
        if (data.dataIndex === filter.dataIndex) {
          return {
            ...data,
            value: filter.value
          };
        } else return data;
      });
    }

    setFiltersData(newFilters);
  };

  const onRemoveFilter = (dataIndex: string) => {
    setFiltersData(filtersData.filter(data => data.dataIndex !== dataIndex));
  };

  const getRows = () => {
    let result: any[] = table.rows || [];
    if (filtersData.length > 0) {
      const { rows, filterMap } = getFilteredRow(filtersData, table.rows || [], table.columns || [], search);
      result = rows;
      filteredMap = filterMap;
    }

    return result;
  };

  const addCol = (index: string) => {
    const colIndex = index.split("_")[1];
    const pos = parseInt(colIndex);
    const newPos = pos === -1 ? 1 : pos + 1;
    const newIndex = `column_${newPos}`;
    const newCol = {
      id: uuid(),
      title: `Column ${newPos}`,
      dataIndex: newIndex,
      index: newPos,
      key: newIndex,
      inputType: "string",
      editable: true,
      readOnly: false
    };

    if (selectedIndexes.length > 0) {
      const newIndexes = selectedIndexes.map(sIndex => {
        const prevPos = parseInt(sIndex.split("_")[1]);
        if (prevPos > pos) {
          return `column_${prevPos + 1}`;
        } else return sIndex;
      });
      setSelectedIndexes(newIndexes);
    }

    // May need to update filterData as well to make sure
    // it refers to the correct column.
    if (filtersData.length) {
      const newFiltersData = filtersData.map(filter => {
        const dataIndex = filter.dataIndex;
        const prevPos = parseInt(dataIndex.split("_")[1]);
        if (prevPos > pos) {
          return {
            ...filter,
            dataIndex: `column_${prevPos + 1}`
          };
        } else return filter;
      });

      setFiltersData(newFiltersData);
    }
    const updatedRows = getUpdatedRows(table.rows || [], newIndex);
    const updatedColumns = getUpdatedColumns(table.columns || [], newCol, pos);

    setTable({ ...table, rows: updatedRows, columns: updatedColumns });
    setUpdateBtnStatus(updateBtnStatus + 1);
  };

  const add = (index: string) => {
    if (index.includes("row")) {
      addRow(index);
    } else {
      addCol(index);
    }
  };

  const setColumnVal = (colIndex: string, action: string, value: any) => {
    const key = action.split("_")[1];
    if (key === "title" && checkForColumnName(table.columns || [], colIndex, value)) {
      notification.error({
        message: "Column name should be unique"
      });
    } else {
      const updatedColumns = (table.columns || []).map((col: any, index: number) => {
        if (colIndex === col.dataIndex) {
          return {
            ...col,
            [key]: value
          };
        }
        return col;
      });

      setTable({ ...table, columns: updatedColumns });
      setUpdateBtnStatus(updateBtnStatus + 1);
    }
  };

  const setCellValue = (rowId: string, value: any) => {
    const [val, rowIndex, colIndex] = rowId.split("_");
    const data = getColData(table.columns || [], parseInt(colIndex));
    if (data) {
      const updatedRows = (table.rows || []).map((row: any, index: number) => {
        if (parseInt(rowIndex) === index) {
          const key = data.dataIndex;
          return {
            ...row,
            [key]: value
          };
        }
        return row;
      });
      setTable({ ...table, rows: updatedRows });
      setUpdateBtnStatus(updateBtnStatus + 1);
    }
  };

  const setName = useCallback((e: any) => {
    const name = e.target.value;
    setTableName(name);
    debounceCheckName(name);
  }, []);

  const deleteRow = (rowId: string) => {
    const [val, rowIndex, colIndex] = rowId.split("_");
    if ((table.rows || []).length > 1) {
      const updatedRows = (table.rows || []).filter((row: any, index: number) => index !== parseInt(rowIndex));
      setTable({ ...table, rows: updatedRows });
      setUpdateBtnStatus(updateBtnStatus + 1);
    } else {
      notification.error({
        message: "Can't delete. At least one row is required"
      });
    }
  };

  const deleteCol = (colId: string) => {
    if ((table.columns || []).length > 1) {
      const updatedColumns = (table.columns || []).filter((col: any, index: number) => col.dataIndex !== colId);
      const updatedRows = (table.rows || []).map((row: any) => {
        delete row[colId];
        return row;
      });
      const updatedFilters = filtersData.filter((filter: any) => filter.dataIndex !== colId);
      const updatedSelectedIndexes = selectedIndexes.filter((selectedIndex: any) => selectedIndex !== colId);
      setFiltersData(updatedFilters);
      setSelectedIndexes(updatedSelectedIndexes);
      setTable({ ...table, columns: updatedColumns, rows: updatedRows });
      setUpdateBtnStatus(updateBtnStatus + 1);
    } else {
      notification.error({
        message: "Can't delete. At least one column is required"
      });
    }
  };

  const setColumnAction = (action: string, colId: string, value?: any) => {
    switch (action) {
      case "col_title":
      case "col_inputType":
      case "col_required":
      case "col_readOnly":
        setColumnVal(colId, action, value);
        break;
      case "deleteCol":
        deleteCol(colId);
        break;
      case "addCol":
        add(colId);
        break;
    }
  };

  const setRowAction = (action: string, rowId: string, value?: any) => {
    switch (action) {
      case "addRow":
        add(rowId);
        break;
      case "deleteRow": {
        const adjusted_rowId = adjustRowId(rowId, tablePage, filteredMap);
        deleteRow(adjusted_rowId);
        break;
      }
      case "cell_val": {
        const adjusted_rowId = adjustRowId(rowId, tablePage, filteredMap);
        setCellValue(adjusted_rowId, value as any);
        break;
      }
    }
  };

  if (tableLoading) {
    return (
      <div style={{ display: "flex", justifyContent: "center", alignItems: "center" }}>
        <Spin />
      </div>
    );
  }

  const theRows = getRows();
  const fixedTablePage = fixTablePageEdgeCase(tablePage, theRows.length);
  if (fixedTablePage.wasFixed) {
    setTablePage(fixedTablePage.truePageNumber);
  }
  const isLastPage = !theRows.length || fixedTablePage.truePageNumber >= Math.ceil(theRows.length / PAGE_SIZE);
  const numItemsOnLastPage = theRows.length ? theRows.length % PAGE_SIZE || PAGE_SIZE : 0;
  const numItemsOnCurrentPage = isLastPage ? numItemsOnLastPage : PAGE_SIZE;
  const totalPagination = {
    pageSize: PAGE_SIZE,
    current: fixedTablePage.truePageNumber,
    onChange: (page: number, pageSize: any) => {
      setTablePage(page);
    },
    onShowSizeChange: () => {},
    showTotal: (total: number, range: any[]) => `${range[0]}-${range[1]} of ${total}`,
    total: theRows?.length || 0,
    size: "small"
  };

  //@ts-ignore
  return (
    <div style={{ display: "flex", flexDirection: "column" }}>
      <ConfigTableFilter
        onFilterChange={onFiltersChange}
        filtersData={filtersData}
        onRemoveFilter={onRemoveFilter}
        columns={table.columns || []}
        rows={table.rows || []}
        selectedIndexes={selectedIndexes}
        setSelectedIndexes={indexes => setSelectedIndexes(indexes)}
      />
      <div
        className={"configure-table-view"}
        style={{ position: "relative", paddingBottom: 0 }}
        onMouseEnter={() => setEdit(true)}
        onMouseLeave={() => setEdit(false)}>
        <EditableContext.Provider value={{ rowsEdit, setRowsEdit: rowEdit }}>
          <div className={"table-action-row"}>
            <div style={{ position: "relative", height: "100%" }}>
              {PAGE_SIZE_ARR.filter((item: any, index: number) => {
                const keep = index <= numItemsOnCurrentPage;
                return keep;
              }).map((row: any, index: number, rows: any[]) => {
                const adjusted_index = index + (fixedTablePage.truePageNumber - 1) * PAGE_SIZE - 1;
                return (
                  <ActionComponent
                    key={index}
                    style={{ top: `calc(${index * 4 + 3}rem)` }}
                    className={"row-dot-pos"}
                    index={`row_${adjusted_index}`}
                    onClick={() => addRow(`row_${adjusted_index}`)}
                  />
                );
              })}
            </div>
          </div>
          <AntRow>
            <AntCol span={24}>
              <div style={{ overflowX: "scroll" }}>
                {!createTableModal && (
                  <ConfigureTable
                    edit={edit}
                    columns={table.columns || []}
                    filtersData={filtersData}
                    rows={theRows}
                    setColumnAction={setColumnAction}
                    setRowAction={setRowAction}
                    setRows={data => {
                      setTable((t: any) => ({ ...t, rows: data }));
                      setUpdateBtnStatus((s: number) => s + 1);
                    }}
                    paginationConfig={totalPagination}
                    setColumns={data => {
                      setTable((t: any) => ({ ...t, columns: data }));
                    }}
                  />
                )}
                {createModal()}
              </div>
              <CSVImportModal
                visible={showImportCSV}
                onCancel={() => {
                  setCreateTableModal(false);
                  setNameFieldBlur(false);
                  setShowImportCSV(false);
                  if (!reImporting && (!tableId || tableName.length === 0)) {
                    setTableName("");
                    props.history.push(`${getBaseUrl()}${CONFIG_TABLE_ROUTES.LIST}`);
                  }
                }}
                closable={false}
                onImportComplete={handleImport}
                getValidateStatus={getValidateStatus}
                getError={getError}
                setNameFieldBlur={setNameFieldBlur}
                setTableName={setName}
                tableName={tableName}
                reImporting={reImporting}
              />
            </AntCol>
          </AntRow>
        </EditableContext.Provider>
      </div>
    </div>
  );
};

const mapStateToProps = (state: any, ownProps: any) => ({
  ...mapRestapiStatetoProps(state),
  configTable: configTableViewState(state, ownProps),
  configTablesCreateState: configTableCreateState(state),
  configTablesUpdateState: configTableUpdateState(state, ownProps),
  page: getPageSettingsSelector(state)
});

const mapDispatchToProps = (dispatch: any) => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapGenericToProps(dispatch),
  ...mapPageSettingsDispatchToProps(dispatch)
});

// @ts-ignore
export default connect(mapStateToProps, mapDispatchToProps)(ConfigurationTablesEditCreate);
