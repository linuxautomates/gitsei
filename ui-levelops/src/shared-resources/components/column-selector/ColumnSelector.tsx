import { Button, Checkbox, Menu, Tooltip } from "antd";
import { ColumnProps } from "antd/lib/table";
import React, { useEffect, useMemo, useState } from "react";
import { toTitleCase } from "utils/stringUtils";
import "./ColumnSelector.scss";

export interface ColumnSelecorProps {
  /** List of all the columns available */
  availableColumns: Array<{ title: string; dataIndex: string }>;
  /** Columns that are current selected */
  visibleColumns: Array<ColumnProps<any>>;
  /** The default columns to be displayed */
  defaultColumns: Array<ColumnProps<any>>;
  /** Method to invoke when the selected columns are updated */
  saveSelectedColumns?: (savedColumns: Array<string>) => void;
}

const ColumnSelector = (props: ColumnSelecorProps) => {
  const { availableColumns, defaultColumns, saveSelectedColumns, visibleColumns } = props;

  const visibleColumnNames = visibleColumns.map((column: any) => column.dataIndex) || [];

  const [selectedColumns, setSelectedColumns] = useState<Set<string>>(new Set(visibleColumnNames));
  const [isValid, setIsValid] = useState<boolean>(true);
  useEffect(() => setIsValid(selectedColumns.size > 0), [selectedColumns]);

  const availableColumnsMap: any = useMemo(
    () =>
      availableColumns.reduce(
        (acc: any, column: { title: string; dataIndex: string }) => ({
          ...acc,
          [column.dataIndex]: column.title
        }),
        {}
      ),
    [availableColumns]
  );

  const onCheckboxSelected = (columnName: string, checked: boolean) => {
    if (checked) {
      setSelectedColumns(new Set(selectedColumns).add(columnName));
    } else {
      const newSet = new Set(selectedColumns);
      newSet.delete(columnName);
      setSelectedColumns(newSet);
    }
  };

  const getName = (columnIndex: string) => {
    let title = availableColumnsMap[columnIndex];
    if (!title) {
      const visibleColumn: any = visibleColumns.find(col => col.dataIndex === columnIndex);
      if (visibleColumn) {
        title = typeof visibleColumn.title === "string" ? visibleColumn.title : visibleColumn.titleForCSV;
        if (!title) {
          title = toTitleCase(visibleColumn.dataIndex);
        }
      }
    }
    return title;
  };

  const resetToDefaultHandler = () => {
    setSelectedColumns(
      new Set(defaultColumns.map((column: any) => column.dataIndex).filter((column: string) => !!column))
    );
  };

  const columns: Set<string> = useMemo(
    () => new Set([...Array.from(selectedColumns), ...availableColumns.map(column => column.dataIndex)]),
    [availableColumns, selectedColumns]
  );

  const saveHandler = () => {
    if (saveSelectedColumns) {
      saveSelectedColumns(Array.from(selectedColumns));
    }
  };

  return (
    <div className="column-selection">
      <Menu>
        <Menu.ItemGroup key={"column-selection-popup"}>
          <div className="scrollable">
            {Array.from(columns).map((column: string) => (
              <div className="column">
                <Checkbox
                  checked={selectedColumns?.has(column)}
                  onChange={e => onCheckboxSelected(column, e.target.checked)}>
                  {getName(column)}
                </Checkbox>
              </div>
            ))}
          </div>
          <div className="footer">
            <Tooltip title={!isValid && "At least one column must be selected"}>
              <Button type="primary" disabled={!isValid} onClick={saveHandler}>
                Save
              </Button>
            </Tooltip>
            <Button type="link" onClick={resetToDefaultHandler}>
              Reset Defaults
            </Button>
          </div>
        </Menu.ItemGroup>
      </Menu>
    </div>
  );
};

export default ColumnSelector;
