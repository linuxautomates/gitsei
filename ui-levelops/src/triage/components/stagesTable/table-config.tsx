import * as React from "react";
import { tableCell } from "../../../utils/tableUtils";
import { Popover } from "antd";
import { Link } from "react-router-dom";
import { AntText } from "../../../shared-resources/components";
import { getBaseUrl, TRIAGE_ROUTES } from "../../../constants/routePaths";

export const tableColumns = [
  {
    title: "Stage/Job",
    dataIndex: "name",
    key: "name",
    render: (item: string, record: any, index: number) => {
      const pathMap = (path: Array<any>, slicer: boolean) => {
        let newPath = path.sort((a: any, b: any) => a.position - b.position);
        if (slicer) {
          newPath = newPath.slice(-3);
        }
        return newPath.map((obj: any, i: number) => {
          let name = `${obj.name} / `;
          if (i === newPath.length - 1) {
            name = obj.name;
          }
          if (obj.type === "CICD_JOB" && obj.id) {
            return <a href={`${getBaseUrl()}${TRIAGE_ROUTES.DETAIL}?id=${obj.id}`}>{name}</a>;
          } else {
            return name;
          }
        });
      };

      if (record.full_path && record.full_path.length > 0) {
        return (
          <Popover overlayStyle={{ maxWidth: "500px" }} content={<AntText>{pathMap(record.full_path, false)}</AntText>}>
            <AntText>
              {record.full_path.length > 3 && "... / "}
              {pathMap(record.full_path, true)}
            </AntText>
          </Popover>
        );
      }

      return item;
    }
  },
  {
    title: "Result",
    dataIndex: "result",
    key: "result",
    render: (item: string, record: any, index: number) => {
      return tableCell("status", item);
    },
    filterType: "multiSelect",
    filterField: "result",
    span: 8,
    options: ["SUCCESS", "FAILURE", "ABORTED", "NOT_BUILT", "UNSTABLE"]
  },
  {
    title: "State",
    dataIndex: "state",
    key: "state",
    filterField: "state",
    filterType: "multiSelect",
    span: 8,
    options: ["FINISHED", "SKIPPED"]
  },
  {
    title: "Matches",
    dataIndex: "hits",
    key: "hits",
    render: (item: any, record: any, index: number) => {
      const matches = (item || []).reduce((acc: number, obj: any) => {
        acc += obj.count;
        return acc;
      }, 0);
      return matches;
    }
  },
  {
    title: "Logs",
    dataIndex: "url",
    key: "url",
    render: (item: string, record: any, index: number) => {
      let fullPath = [...record.full_path] || [];
      let type = undefined;
      let idType = "stage_id";
      if (fullPath && fullPath.length > 0) {
        fullPath = fullPath.sort((a: any, b: any) => a.position - b.position);
        const rec = fullPath.pop();
        if (rec) {
          type = rec.type;
          if (type) {
            idType = type === "CICD_STAGE" ? "stage_id" : "job_id";
          }
        }
      }

      return (
        <AntText>
          <a href={item} target={"_blank"} rel="noopener noreferrer">
            View Logs
          </a>
          {record.result === "FAILURE" && type && type === "CICD_STAGE" && (
            <Link
              style={{ marginLeft: "1rem" }}
              rel="noopener noreferrer"
              to={`${getBaseUrl()}/failure-logs?${idType}=${record.id}&log_name=${record.name}`}
              target={"_blank"}>
              View Raw Logs
            </Link>
          )}
        </AntText>
      );
    }
  }
];
