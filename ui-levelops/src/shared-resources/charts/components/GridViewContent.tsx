import React, { useCallback, useMemo } from "react";
import { AntIcon, AntText, AntTooltip } from "../../components";
import { capitalize } from "lodash";
import { FILES_REPORT_ROOT_FOLDER_KEY } from "dashboard/constants/helper";
import { isFile } from "../grid-view-chart/util";

export const GridViewContent = (props: any) => {
  const { x, y, width, height, title, subTitle, dataKey, onClick, total, color, repo_id, previewOnly } = props;

  const root_folder_key = props[FILES_REPORT_ROOT_FOLDER_KEY];

  const percent = useMemo(() => (props[dataKey] * 100) / total, [props?.[dataKey], total]);

  const iconType = useMemo(() => (isFile(subTitle?.value) ? "file" : "folder"), [subTitle]);

  const rectStyle = useMemo(() => ({ fill: color }), [color]);

  const renderTooltip = useMemo(() => {
    let label;
    switch (dataKey) {
      case "count":
        label = "Total Commits";
        break;
      case "total_issues":
        label = "Total Issues";
        break;
      case "total_cases":
      case "total_tickets":
        label = "Total Cases";
        break;
      default:
        label = dataKey;
    }
    return (
      <div className="tooltip-container flex direction-column">
        {title && Object.keys(title).length > 0 && <span>{`${title.label}: ${title.value}`}</span>}
        {subTitle && Object.keys(subTitle).length > 0 && <span>{`${subTitle.label}: ${subTitle.value}`}</span>}
        {props?.salesforce_case_numbers && (
          <>
            <span>Salesforce Cases: </span>
            <div>
              {props["salesforce_case_numbers"].map((item: any, index: number) => {
                return <span key={`salesforce_case_numbers-${index}`} className="mr-5">{`${item},`}</span>;
              })}
            </div>
          </>
        )}
        {props?.total_cases && !root_folder_key && (
          <span>
            {capitalize("total_cases".replace(/_/g, " "))}: {props.total_cases}
          </span>
        )}
        {props?.zendesk_ticket_ids && (
          <>
            <span>Zendesk Tickets: </span>
            <div>
              {props?.zendesk_ticket_ids.map((item: any, index: number) => {
                return <span key={`zendesk_ticket_ids-${index}`} className="mr-5">{`${item},`}</span>;
              })}
            </div>
          </>
        )}
        {dataKey && props?.[dataKey] && (
          <span>
            {label}: {props?.[dataKey]}
          </span>
        )}
      </div>
    );
  }, [
    dataKey,
    title,
    subTitle,
    root_folder_key,
    props?.salesforce_case_numbers,
    props?.total_cases,
    props?.zendesk_ticket_ids,
    props?.[dataKey]
  ]);

  const renderContent = useMemo(() => {
    if (percent > 5) {
      let label;
      switch (dataKey) {
        case "count":
          label = "Commits";
          break;
        case "total_issues":
          label = "Issues";
          break;
        case "total_cases":
        case "total_tickets":
          label = "Cases";
          break;
        default:
          label = dataKey;
      }

      return (
        <div className="content">
          <div className="path-container">
            <AntIcon className="path-icon" type={iconType} theme={"outlined"} />
            <AntText className="title">{subTitle?.value}</AntText>
          </div>
          <AntText className="data">{`${label}: ${props[dataKey]}`}</AntText>
        </div>
      );
    }

    if (width > 24 && height > 24) {
      return (
        <div className="stat-container">
          <AntText className="stat stat-normal">{props[dataKey]}</AntText>
        </div>
      );
    }

    if (width > 12 && height > 12) {
      return (
        <div className="stat-container">
          <AntText className="stat stat-small">{props[dataKey]}</AntText>
        </div>
      );
    }

    return null;
  }, [width, height, subTitle, dataKey, props?.[dataKey]]);

  const onMapClicked = useCallback(() => {
    if (!isFile(subTitle?.value) && root_folder_key) {
      onClick &&
        onClick({
          value: root_folder_key,
          repo_id: repo_id || ""
        });
    }
  }, [subTitle, root_folder_key, onClick]);

  return (
    <AntTooltip title={previewOnly ? null : renderTooltip}>
      <g onClick={onMapClicked} className="cursor-pointer">
        <rect x={x} y={y} width={width} height={height} style={rectStyle} />
        {dataKey && props?.[dataKey] && (
          <foreignObject x={x} y={y} width={width} height={height}>
            {renderContent}
          </foreignObject>
        )}
      </g>
    </AntTooltip>
  );
};
