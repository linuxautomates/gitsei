import { Table } from "antd";
import { convertFromTableSchema } from "configuration-tables/helper";
import React, { useState, useEffect } from "react";
import { get } from "lodash";
import PluginResultReportTableCol from "./PluginResultReportTableCol";

interface CSVReportTableProps {
  resultData: any;
  print: boolean;
}

const components = {
  header: {
    cell: PluginResultReportTableCol
  }
};

const CSVReportTable: React.FC<CSVReportTableProps> = (props: CSVReportTableProps) => {
  const [tableData, setTableData] = useState<any | undefined>();
  const [resizeFactorMap, setResizeFactorMap] = useState<any>({});
  const [initialWidthMap, setInitialWidthMap] = useState<any>({});

  useEffect(() => {
    setTableData(convertFromTableSchema(props.resultData || {}));
  }, [props.resultData]);

  const getWidth = () => {
    return `calc(100% + ${Object.values(resizeFactorMap)
      .filter((val: any) => val >= 0)
      .reduce((acc: number, item: any) => acc + item, 0)}px)`;
  };

  const getInitialWidth = (index: string) => {
    let initialWidth = get(initialWidthMap, [index], undefined);
    if (initialWidth) {
      return initialWidth;
    } else {
      const ele = document.getElementById(index);
      if (ele) {
        initialWidth = ele.getBoundingClientRect().width;
        setInitialWidthMap((iMap: any) => ({ ...iMap, [index]: initialWidth }));
        return initialWidth;
      }
    }
  };

  const handleResize = (index: string, event: any, data: any) => {
    const width = get(data, ["size", "width"], 0);
    const updatedColumns = (tableData?.columns || []).map((col: any, colIndex: any) => {
      if (col.dataIndex === index) {
        return {
          ...col,
          width
        };
      } else return col;
    });

    const factor = width - getInitialWidth(index);

    if (factor < 0) {
      let newInitials = initialWidthMap;
      (tableData?.columns || []).forEach((col: any) => {
        if (col.dataIndex !== index) {
          const ele = document.getElementById(col.dataIndex);
          if (ele) {
            newInitials = {
              ...newInitials,
              [`${col.dataIndex}`]: ele.getBoundingClientRect().width
            };
          }
        }
      });
      setInitialWidthMap(newInitials);
    }

    setResizeFactorMap((rMap: any) => ({ ...rMap, [index]: factor }));

    setTableData((tData: any) => ({ ...tData, columns: updatedColumns }));
  };

  const mergedColumns = (tableData?.columns || []).map((col: any, index: number) => {
    return {
      ...col,
      onHeaderCell: (hProps: any) => ({
        width: hProps.width || getInitialWidth(col.dataIndex),
        dataIndex: col.dataIndex,
        title: col.title,
        onResize: (e: any, data: any) => handleResize(col.dataIndex, e, data)
      })
    };
  });

  let newProps: any = {};

  if (!props.print) {
    newProps["components"] = components;
  }

  return (
    <div style={{ overflowX: "scroll", width: "100%" }}>
      {tableData && (
        <div style={{ width: getWidth() }}>
          <Table
            {...newProps}
            rowKey={(record, index) => `${index}`}
            bordered
            columns={props.print ? (Array.isArray(tableData?.columns) ? tableData?.columns || [] : []) : mergedColumns}
            pagination={false}
            dataSource={Array.isArray(tableData?.rows) ? tableData?.rows || [] : []}
          />
        </div>
      )}
    </div>
  );
};

export default CSVReportTable;
