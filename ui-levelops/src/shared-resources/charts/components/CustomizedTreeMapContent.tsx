import React from "react";
import { AntTooltip } from "../../components";
import { capitalize, get } from "lodash";
import { FILES_REPORT_ROOT_FOLDER_KEY } from "dashboard/constants/helper";
import { SCM_FILES_REPORT } from "dashboard/constants/applications/names";

const SIZE_PERCENT = 3;

export const CustomizedTreeMapContent = (props: any) => {
  const { x, y, width, height, title, color, subTitle, dataKey, onClick, total, reportType, previewOnly } = props;

  const onMapClicked = () => {
    if (dataKey === "count") {
      if (props[FILES_REPORT_ROOT_FOLDER_KEY]) {
        onClick(props[FILES_REPORT_ROOT_FOLDER_KEY]);
      } else {
        onClick({
          cicd_job_id: props["cicd_job_id"],
          label: subTitle ? subTitle.value : ""
        });
      }
      return;
    }

    if (props[FILES_REPORT_ROOT_FOLDER_KEY]) {
      onClick(props[FILES_REPORT_ROOT_FOLDER_KEY]);
      return;
    }

    if (reportType === SCM_FILES_REPORT) {
      onClick({ repo_ids: props["repo_id"], filename: props["filename"] });
      return;
    }
    onClick(props["repo_id"] || "");
  };

  const renderTooltip = () => {
    return (
      <div className="flex direction-column">
        {title && Object.keys(title).length > 0 && (
          <span style={{ color: "#fff" }}>{`${title.label}: ${title.value}`}</span>
        )}
        {subTitle && Object.keys(subTitle).length > 0 && (
          <span style={{ color: "#fff" }}>{`${subTitle.label}: ${subTitle.value}`}</span>
        )}
        {props && props["jira_issue_keys"] && (
          <>
            <span style={{ color: "#fff" }}>Jira Issue Keys: </span>
            <div>
              {props["jira_issue_keys"].map((item: any, index: number) => {
                return (
                  <span key={`jira_issue_keys-${index}`} style={{ color: "#fff" }} className="mr-5">{`${item},`}</span>
                );
              })}
            </div>
          </>
        )}
        {props && props["salesforce_case_numbers"] && (
          <>
            <span style={{ color: "#fff" }}>Salesforce Cases: </span>
            <div>
              {props["salesforce_case_numbers"].map((item: any, index: number) => {
                return (
                  <span
                    key={`salesforce_case_numbers-${index}`}
                    style={{ color: "#fff" }}
                    className="mr-5">{`${item},`}</span>
                );
              })}
            </div>
          </>
        )}
        {props && props["total_cases"] && !props[FILES_REPORT_ROOT_FOLDER_KEY] && (
          <span style={{ color: "#fff" }}>
            {capitalize("total_cases".replace(/_/g, " "))}: {props["total_cases"]}
          </span>
        )}
        {props && props["zendesk_ticket_ids"] && (
          <>
            <span style={{ color: "#fff" }}>Zendesk Tickets: </span>
            <div>
              {props["zendesk_ticket_ids"].map((item: any, index: number) => {
                return (
                  <span
                    key={`zendesk_ticket_ids-${index}`}
                    style={{ color: "#fff" }}
                    className="mr-5">{`${item},`}</span>
                );
              })}
            </div>
          </>
        )}
        {props && dataKey && props[dataKey] && (
          <span style={{ color: "#fff" }}>
            {capitalize(dataKey.replace(/_/g, " "))}: {props[dataKey]}
          </span>
        )}
      </div>
    );
  };

  const renderFileName = (value: string) => {
    if (value && value.length > 15) {
      return (
        <>
          {value.slice(0, 15)}
          <tspan x={x + width / 2} dy="1.2em">
            {value.slice(15, value.length)}
          </tspan>
        </>
      );
    } else return value;
  };

  const percent = (props[dataKey] / total) * 100;

  let fileNameY = y + height / 2;
  let changesY = y + height / 2;

  if (percent > SIZE_PERCENT) {
    fileNameY = fileNameY - 16;
    changesY = changesY + 4;

    if (get(subTitle, ["value", "length"], 0) > 15) {
      changesY = changesY + 12;
    }
  }

  return (
    <AntTooltip title={previewOnly ? null : renderTooltip}>
      <g onClick={onMapClicked} style={{ cursor: "pointer" }}>
        <rect
          x={x}
          y={y}
          width={width}
          height={height}
          style={{
            fill: color
          }}
        />
        {props && dataKey && props[dataKey] && (
          <>
            {percent > 3 && (
              <text
                style={{
                  overflowWrap: "break-word",
                  whiteSpace: "nowrap",
                  textOverflow: "ellipsis"
                }}
                x={x + width / 2}
                y={fileNameY}
                textAnchor="middle"
                fill="#fff"
                fontSize={12}>
                {subTitle && Object.keys(subTitle).length > 0 && renderFileName(subTitle.value)}
              </text>
            )}
            <text x={x + width / 2} y={changesY} textAnchor="middle" fill="#fff" fontSize={12}>
              {props[dataKey]}
            </text>
          </>
        )}
      </g>
    </AntTooltip>
  );
};
