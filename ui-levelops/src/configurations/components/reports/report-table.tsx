import { Table } from "antd";
import React, { useMemo } from "react";
import { AntCard, AntCol, AntRow, AntText, AntTitle } from "shared-resources/components";
import { tmtTableColumns, prtTableColumns, synkColumns, nccColumns, tenableColumns } from "./table-config";

interface ReportTableProps {
  data: any[];
  type: string;
  print?: boolean;
  details?: (id: string) => void;
  title?: string;
}

const ReportTable: React.FC<ReportTableProps> = (props: ReportTableProps) => {
  let { data, type, print, details, title } = props;

  if (!title) title = "Top Issues";

  const columns = useMemo(
    () =>
      type === "report_ms_tmt"
        ? tmtTableColumns
        : type === "snyk"
        ? synkColumns
        : type === "TENABLE"
        ? tenableColumns
        : type === "report_nccgroup"
        ? nccColumns
        : prtTableColumns,
    [type]
  );

  const printableTable = useMemo(() => {
    const noellipsisColums = (columns as any).map((column: any) => {
      if (column.key === "meta" && type === "report_praetorian") {
        return {
          ...column,
          ellipsis: false,
          render: (item: any) => {
            const scores = Object.keys(item.score).map(score => {
              return <AntCol span={24}>{`${score} ${item.score[score].value}`}</AntCol>;
            });
            return (
              <AntRow gutter={[3, 3]} justify={"space-between"}>
                {scores}
              </AntRow>
            );
          }
        };
      }
      return {
        ...column,
        ellipsis: false
      };
    });
    return (
      <>
        <AntTitle level={4}>{title}</AntTitle>
        <Table dataSource={data} columns={noellipsisColums} size={"small"} pagination={false} />
      </>
    );
  }, [data, title, columns]);

  const nonPrintableTable = useMemo(() => {
    const mappedColumns = (columns as any).map((column: any) => {
      if (column.dataIndex === "id" && ["report_praetorian", "report_nccgroup"].includes(type) && !print) {
        return {
          ...column,
          render: (item: any) => {
            return (
              <AntText>
                <a
                  href={"!#"}
                  onClick={e => {
                    e.preventDefault();
                    details?.(item);
                  }}>
                  {item}
                </a>
              </AntText>
            );
          }
        };
      }
      if ((type === "snyk" || type === "TENABLE") && column.dataIndex === "id") {
        return {
          ...column,
          render: (item: any, record: any) => {
            return (
              <AntText>
                <a
                  href={"!#"}
                  onClick={e => {
                    e.preventDefault();
                    details?.(record.id);
                  }}>
                  {item}
                </a>
              </AntText>
            );
          }
        };
      }
      return column;
    });

    return (
      <AntCard title={title}>
        <Table dataSource={data} columns={mappedColumns} size={"middle"} pagination={{ pageSize: 20 }} />
      </AntCard>
    );
  }, [data, title, columns]);

  if (print === true) return printableTable;

  return nonPrintableTable;
};

export default React.memo(ReportTable);
