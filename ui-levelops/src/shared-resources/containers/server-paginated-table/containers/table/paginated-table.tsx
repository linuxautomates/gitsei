import { TableLocale } from "antd/lib/table";
import React from "react";
import { AntTable } from "shared-resources/components";

// TODO: use proper types.
export interface PaginatedTableProps {
  loading?: boolean;
  expandedRowRender?: boolean;
  showPageSizeOptions?: boolean;
  pageSize?: number;
  onRow?: any;
  rowKey?: string;
  rowSelection?: any;
  bordered?: boolean;
  columns: any;
  scroll?: any;
  onPageChange?: (page: number) => void;
  page?: number;
  rowClassName?: (record: any, index: number) => string;
  expandIconColumnIndex?: number;
}

interface DefaultProps extends PaginatedTableProps {
  dataSource: any[];
  className: any;
  expandedRowKeys: any;
  totalPages: any;
  totalRecords: any;
  onChange: any;
  onExpand: any;
  onPageSizeChange: any;
  size: string;
  hasCustomPagination: boolean;
  locale: TableLocale;
  showCustomChanger?: boolean;
}

const PaginatedTable: React.FC<DefaultProps> = (props: DefaultProps) => {
  const {
    className,
    size,
    pageSize,
    page,
    onRow,
    rowKey,
    bordered,
    scroll,
    columns,
    dataSource,
    loading,
    expandedRowRender,
    expandedRowKeys,
    totalPages,
    totalRecords,
    rowSelection,
    rowClassName,
    expandIconColumnIndex,
    hasCustomPagination,
    locale,
    showCustomChanger
  } = props;

  return (
    <AntTable
      className={className}
      hasCustomPagination={hasCustomPagination}
      dataSource={dataSource}
      loading={loading}
      expandedRowRender={expandedRowRender}
      expandedRowKeys={expandedRowKeys}
      columns={columns}
      bordered={bordered}
      scroll={scroll}
      pageSize={pageSize}
      page={page}
      onRow={onRow}
      rowSelection={rowSelection}
      totalPages={totalPages}
      totalRecords={totalRecords}
      size={size}
      rowKey={rowKey}
      onChange={props.onChange}
      onExpand={props.onExpand}
      onPageChange={props.onPageChange}
      onPageSizeChange={props.onPageSizeChange}
      rowClassName={rowClassName}
      showPageSizeOptions={props.showPageSizeOptions}
      expandIconColumnIndex={expandIconColumnIndex}
      expandIconAsCell={!expandIconColumnIndex}
      pagination={false}
      locale={locale}
      showCustomChanger={showCustomChanger}
    />
  );
};

export default React.memo(PaginatedTable);
