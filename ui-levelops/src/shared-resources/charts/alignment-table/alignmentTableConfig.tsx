import React from "react";
import { Slider, Tag } from "antd";
import { ColumnProps } from "antd/lib/table";
import { alignmentCategoryConfig, categoryAlignmentConfig } from "dashboard/dashboard-types/BAReports.types";
import { allignmentStatus } from "dashboard/constants/enums/jira-ba-reports.enum";
import {
  alignmentStatusMapping,
  alignmentToSVGMapping
} from "dashboard/constants/bussiness-alignment-applications/constants";

export const alignmentTableConfig: ColumnProps<any>[] = [
  {
    title: "Category",
    dataIndex: "name",
    key: "name",
    width: "15%",
    render: (item: string, record: categoryAlignmentConfig) => {
      return <Tag color={record?.config?.color}>{item}</Tag>;
    }
  },
  {
    title: "Ideal Range",
    dataIndex: "config",
    key: "ideal_range",
    width: "15%",
    render: (item: alignmentCategoryConfig) => {
      return <div className="ideal-range">{`${item?.ideal_range?.min}% - ${item?.ideal_range?.max}%`}</div>;
    }
  },
  {
    title: "",
    dataIndex: "config",
    key: "slider-score",
    width: "35%",
    render: (item: alignmentCategoryConfig, record: categoryAlignmentConfig, index: number) => {
      const score = (item?.allocation ?? 0) * 100;
      const marks = {
        [score]: {
          style: {
            marginTop: "-4.5rem"
          },
          label: (
            <div className="slider-score-mark" style={{ backgroundColor: item?.color }}>
              <div className="score-mark">{`${Math.round(score)}%`}</div>
            </div>
          )
        }
      };

      return (
        <Slider
          marks={marks}
          className={`score-slider ${(record?.id || "").concat(index + "")}`}
          range
          disabled
          defaultValue={[item?.ideal_range?.min ?? 0, item?.ideal_range?.max ?? 0]}
        />
      );
    }
  },
  {
    title: "",
    dataIndex: "config",
    key: "alignment_status",
    width: "20%",
    render: (item: alignmentCategoryConfig) => {
      const alignmentStatus: allignmentStatus = (alignmentStatusMapping as any)[item.alignment_score];
      return (
        <div className="alignment-status-container">
          <div className="avatar">
            <img src={alignmentToSVGMapping()[alignmentStatus]} alt="not found" />
          </div>
          <div className="status">{alignmentStatus}</div>
        </div>
      );
    }
  }
];
