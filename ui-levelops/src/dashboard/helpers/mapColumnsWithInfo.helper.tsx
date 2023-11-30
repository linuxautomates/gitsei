import { StatsMap, StatsDescription } from "constants/stat.description";
import { map, isArray } from "lodash";
import React from "react";
import { TitleWithInfo } from "shared-resources/components";

export const mapColumnsWithInfo = (columns: any[], columnsWithInfo: any[]) => {
  if (columnsWithInfo && isArray(columnsWithInfo) && columnsWithInfo.length) {
    return map(columns || [], column => {
      const { dataIndex } = column;
      if (columnsWithInfo.includes(dataIndex) && Object.values(StatsMap).includes(dataIndex)) {
        return {
          ...column,
          title: <TitleWithInfo title={column.title || ""} description={(StatsDescription as any)[dataIndex]} />
        };
      }
      return column;
    });
  }
  return columns;
};

export const mapReportColumnWithInfo = (columns: any[], columnsWithInfo: any) => {
  if (columnsWithInfo && Object.keys(columnsWithInfo).length) {
    return map(columns || [], column => {
      const { dataIndex } = column;
      if (Object.keys(columnsWithInfo).includes(dataIndex)) {
        return {
          ...column,
          title: <TitleWithInfo title={column.title || ""} description={columnsWithInfo[dataIndex]} />
        };
      }
      return column;
    });
  }
  return columns;
};
