import React, { ReactElement } from "react";
import { AntPagination, EmptyWidget } from "..";
import "./html-table.scss";

export type HtmlTableColumnProp = {
  title: string;
  dataIndex: string;
  key: string;
  width: string;
  textAlign: "start" | "end" | "left" | "right" | "center" | "justify" | "match-parent";
  render: (item: string, record: any, options?: any) => ReactElement;
  style?: any;
  hideData?: boolean;
};

export type PaginationOptionsProps = {
  pageSize?: number;
  page?: number;
  totalRecords?: number;
  onPagnationChange?: (page: number, pageSize?: number | undefined) => void;
};

export type HtmlTableProps = {
  columns: Array<HtmlTableColumnProp>;
  dataSource: Array<any>;
  firstColumnSpan: number;
  className?: string;
  pagination?: PaginationOptionsProps;
};

const Header = (props: any) => (
  <tr>
    {props.columns.map((column: any) => (
      <th
        className="header"
        style={{
          width: column.width ?? "",
          textAlign: column.textAlign ?? "left"
        }}>
        {column.title}
      </th>
    ))}
  </tr>
);

const cell = (column: HtmlTableColumnProp, rowData: any, rowSpan: number) => (
  <td className="cell" style={{ ...(column.style || {}), textAlign: column.textAlign ?? "left" }} rowSpan={rowSpan}>
    {column.hideData ? (
      <span className="hiddenData">{column.render(rowData[column.dataIndex], rowData, { hideData: true })}</span>
    ) : column.render ? (
      column.render(rowData[column.dataIndex], rowData)
    ) : (
      <span>{rowData[column.dataIndex]}</span>
    )}
  </td>
);

const Rows = (props: HtmlTableProps) => {
  let counter = 1;
  return (
    <>
      {props.dataSource.map((rowData: any) => {
        counter = counter === 1 ? props.firstColumnSpan : counter - 1;
        const isNewSet = counter === props.firstColumnSpan;
        return (
          <tr style={{ borderTop: isNewSet ? "1px solid #DCDFE4" : "" }}>
            {props.columns.map((column: HtmlTableColumnProp, index: number) => {
              if (index !== 0 || isNewSet) {
                const rowSpan = index ? 1 : props.firstColumnSpan || 1;
                return cell(column, rowData, rowSpan);
              } else {
                return null;
              }
            })}
          </tr>
        );
      })}
    </>
  );
};

const HtmlTable = (props: HtmlTableProps) => (
  <>
    {!props.dataSource || !props.dataSource.length || !props.columns || !props.columns.length ? (
      <div className={props.className}>
        <EmptyWidget />
      </div>
    ) : (
      <>
        <table className={`html-table ${props.className}`}>
          <Header columns={props.columns} />
          <Rows {...props} />
        </table>
        {props.pagination && (
          <AntPagination
            pageSize={props.pagination.pageSize}
            current={props.pagination.page}
            total={props.pagination.totalRecords}
            showPageSizeOptions={false}
            onPageChange={props.pagination.onPagnationChange}
          />
        )}
      </>
    )}
  </>
);

export default HtmlTable;
