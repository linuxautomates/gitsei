import { Button, Input, notification } from "antd";
import { debounce, isEqual } from "lodash";
import React, { useCallback, useEffect, useMemo, useState, useRef } from "react";
import { AntTable, CustomFormItemLabel } from "shared-resources/components";

interface PopupDynamicPaginatedTableProps {
  onOk: (data: any, update: boolean) => void;
  onCancel: () => void;
  filterValueLoading: boolean;
  dataSource: any[];
  selectedValues: any[];
  tableHeader: string;
  columns: any[];
  metaData?: any;
  valueKey?: string;
  isMultiSelect: boolean;
  handlePaginatedFilters: (data: any) => void;
}

const defaultValue = { page: 1, pageSize: 10, searchValue: "" };

const PopupDynamicPaginatedTable: React.FC<PopupDynamicPaginatedTableProps> = (
  props: PopupDynamicPaginatedTableProps
) => {
  const [selectedRowsKeys, setSelectedRowsKeys] = useState<any[]>([]);
  const [selectedRows, setSelectedRows] = useState<any[]>([]);
  const [pageFilters, setPageFilters] = useState<any>(defaultValue);
  const searchValueRef = useRef<string>("");

  const denounceUpdateFilters = useCallback(debounce(props.handlePaginatedFilters, 300), []);

  useEffect(() => {
    if (!isEqual(pageFilters.searchValue, searchValueRef.current)) {
      searchValueRef.current = pageFilters.searchValue;
      denounceUpdateFilters(pageFilters);
    } else {
      props.handlePaginatedFilters(pageFilters);
    }
  }, [pageFilters]);

  useEffect(() => setSelectedRowsKeys(props.selectedValues || []), [props.selectedValues]);

  const onSelectChange = useCallback(
    (selectedRowKeys: any[], selectedRows: any[]) => {
      if (!props.isMultiSelect && selectedRows.length > 1) {
        notification.error({ message: `${props.tableHeader} is a single select filter` });
      } else {
        setSelectedRowsKeys(selectedRowKeys);
        setSelectedRows(selectedRows);
        props.onOk(selectedRows, false);
      }
    },
    [props.isMultiSelect, props.tableHeader]
  );

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedRowsKeys,
      onChange: onSelectChange,
      columnWidth: "2%"
    }),
    [selectedRowsKeys, onSelectChange]
  );

  const header = useMemo(() => <CustomFormItemLabel label={props.tableHeader} />, [props.tableHeader]);

  const footer = useMemo(
    () => (
      <div className="flex justify-end p-10 mt-20">
        <Button className="mr-10" onClick={props.onCancel}>
          Cancel
        </Button>
        <Button type="primary" onClick={() => props.onOk(selectedRows, true)}>
          Select
        </Button>
      </div>
    ),
    [selectedRowsKeys]
  );

  const containerStyle = useMemo(() => ({ width: "30rem" }), []);
  const tableStyle = useMemo(() => ({ overflowY: "scroll", maxHeight: "30rem" }), []);

  return (
    <div className="flex direction-column" style={containerStyle}>
      <div
        className="flex justify-space-between align-center"
        style={{
          margin: "0.5rem 1.5rem 1rem"
        }}>
        {header}
        <Input
          className="w-40"
          placeholder="Search"
          value={pageFilters?.searchValue}
          onChange={e => setPageFilters({ ...pageFilters, page: 1, searchValue: e.target.value })}
        />
      </div>
      <div style={tableStyle as any}>
        <AntTable
          rowKey={(row: any) => row[props.valueKey || ""] || row["id"] || row["key"]}
          size="small"
          hasCustomPagination
          dataSource={props.dataSource || []}
          loading={props.filterValueLoading}
          columns={props.columns}
          page={pageFilters.page || 1}
          pageSize={pageFilters.pageSize || 10}
          totalRecords={props.metaData?.total_count || (props.dataSource || []).length}
          rowSelection={rowSelection}
          onPageSizeChange={(pageSize: any) => setPageFilters({ ...pageFilters, pageSize, page: 1 })}
          onPageChange={(page: number) => setPageFilters({ ...pageFilters, page })}
        />
      </div>
      {footer}
    </div>
  );
};

export default PopupDynamicPaginatedTable;
