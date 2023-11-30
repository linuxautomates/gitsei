import React from "react";
import { Link } from 'react-router-dom';
import queryString from "query-string";
import DummyLink from "../components/DummyLink/DummyLink";
import { checkmarkButton } from "./buttonUtils";
import {
  AntBadge,
  AntCol,
  AntProgress,
  AntRow,
  AntTag,
  AntText,
  SeverityTag,
  AvatarWithText,
  NameAvatarList,
  AntTooltip
} from "shared-resources/components";
import { SvgIcon } from "../shared-resources/components";
import { Tag, Tooltip } from "antd";
import { v1 as uuid } from "uuid";
import { convertEpochToDate, DateFormats } from "./dateUtils";
import {
  priorityMappping,
  PriorityOrderMapping,
  staticPriorties
} from "shared-resources/charts/jira-prioirty-chart/helper";
import moment from "moment";

const colorList = ["red", "blue", "green", "orange", "purple", "cyan", "magenta", "volcano", "lime", "geekblue"];
let colorIndex = 0;
let tagColorMapping = {};

const addTagToMapping = tag => {
  const color = colorList[colorIndex % 10];
  tagColorMapping = {
    ...tagColorMapping,
    [tag]: color
  };
  colorIndex++;
  return color;
};

export const getColor = tag => {
  const color = tagColorMapping[tag];
  if (!color) {
    return addTagToMapping(tag);
  }
  return color;
};
function _buildQuery(href, filters) {
  return href.concat("?").concat(queryString.stringify(filters));
}

export const renderTagText = text => {
  if (!text) return "";
  return text.length > 15 ? text.slice(0, 15).concat("...") : text;
};

export function _getProgressClass(value) {
  if (value >= 0 && value <= 30) {
    return "active";
  } else if (value > 30 && value <= 70) {
    return "normal";
  } else if (value > 70 && value < 100) {
    return "normal";
  }
  if (value === 100) {
    return "success";
  }
  return "active";
}

function _getSecStatus(value) {
  if (value >= 0 && value <= 3) {
    return (
      <span role="img" aria-label="danger" className="text-danger">
        &#128308;
      </span>
    );
  } else if (value > 3 && value <= 7) {
    return (
      <span role="img" aria-label="warning" className="text-warning">
        &#x26AB;
      </span>
    );
  } else if (value > 7) {
    return (
      <span role="img" aria-label="danger" className="text-danger">
        &#128308;
      </span>
    );
  }
  return "";
}

export const updatedAtColumn = (name = "updated_at", title = "Updated On", filters = {}) => ({
  title,
  key: name,
  dataIndex: name,
  width: "10%",
  render: props => tableCell("created_at", props, undefined, filters)
});

export const nameColumn = (href, key = "name", width = 250, id = "id") => ({
  title: <span className={"pl-10"}>Name</span>,
  key,
  dataIndex: key,
  width: width,
  ellipsis: true,
  render: (item, record, index) => {
    return (
      <AntText className={"pl-10"}>
        <Link className={"ellipsis"} to={`${href}=${record[id]}`}>
          {item}
        </Link>
      </AntText>
    );
  }
});

export const actionsColumn = () => ({
  title: "Actions",
  key: "id",
  dataIndex: "id",
  width: 100,
  align: "center",
  fixed: "right"
});

export const tagsColumn = () => ({
  title: "Tags",
  key: "tags",
  dataIndex: "tags",
  render: (item, record, index) => {
    return tableCell("tags", item);
  }
});

export function tableCell(header, value, href = "#", filters = {}) {
  if (value === null) {
    return "";
  }
  switch (header) {
    case "default":
      if (value === true) {
        return <AntTag color={"blue"}>DEFAULT</AntTag>;
      } else {
        return "";
      }
    case "libraries":
    case "frameworks":
    case "files":
      if (Array.isArray(value)) {
        let result = [];
        value.forEach(element => {
          result.push(
            <tr key={element}>
              <td>
                <DummyLink href="#">{element}</DummyLink>
              </td>
            </tr>
          );
        });
        return <table>{result}</table>;
      }
      return <a>{value}</a>;
    case "link":
      return <a href={value}>{value}</a>;
    case "file_changes_count":
    case "configs_count":
    case "files_ct":
    case "configs_ct":
      let url = _buildQuery(href, filters);
      return <a href={url}>{value}</a>;
    case "name":
      return (
        <a href={href}>
          <h6>{value}</h6>
        </a>
      );
    case "changes":
    case "security_work":
    case "tickets_by_stage":
      let result = [];
      Object.keys(value).forEach((key, index) => {
        result.push(
          <tr key={key}>
            <td align="right">{(key || "").toUpperCase()}</td>
            <td align="right">{value[key]}</td>
          </tr>
        );
        result.push(
          <tr key={`${key}_${index}`}>
            <td>
              <hr />
            </td>
            <td>
              <hr />
            </td>
          </tr>
        );
      });
      return (
        <table width="100%" className="table-responsive">
          <tbody>{result}</tbody>
        </table>
      );
    //return(buildTableChips(value));
    case "repos":
    case "tags":
      if (Array.isArray(value)) {
        if (value.length === 0) {
          return "";
        }
        let result = value.map((val, index) => <AntTag key={index}>{val}</AntTag>);
        return (
          <div className={"ellipsis"}>
            <Tooltip title={value.join(",")}>{result}</Tooltip>
          </div>
        );
      }
      return value;
    case "convertBooleanToString":
      return  (typeof value === "boolean") ? value.toString() : value;
    case "severity":
    case "priority":
      // @ts-ignore
      if (value === "null") return "";
      if (!priorityMappping.includes(value)) {
        if (parseInt(value) in staticPriorties) {
          value = staticPriorties[value];
        }
      }
      return value ? <SeverityTag severity={value.toLowerCase()} /> : "";
    case "actions":
    case "technologies":
      //if (Array.isArray(value)) {
      //	return (<Chips color="default" chips={value} />);
      //}
      return value;
    case "due_date":
    case "due_at":
    case "release_date":
    case "created_at":
    case "updated_on":
    case "last_updated":
    case "timestamp":
    case "created_at_epoch":
      if (value === undefined || value === 0) {
        return "";
      }

      const secondsSinceEpoch = Math.round(Date.now() / 1000);
      if (value > secondsSinceEpoch) {
        // it is in milliseconds need to convert to second, moment.unix() accepts seconds only
        value = Math.round(value / 1000);
      }
      let date = convertEpochToDate(value, DateFormats.DAY, !!filters.useUTC);
      return date;
    //case 'type':
    //	return (buildChips({ [value]: '' }));
    // case "severity":
    //     return(buildChips({[value]:""}));
    case "production":
      if (value) {
        return checkmarkButton({});
      }
      return "";
    case "quiz_progress":
      return (
        <p>
          <AntProgress percent={value} status={_getProgressClass(value)} />
        </p>
      );
    case "progress":
      return (
        <div>
          <legend>
            <h6>
              {`Security Status`} &nbsp; {_getSecStatus(value.score)}
            </h6>
          </legend>
          <p>
            <h6>SAST</h6>

            <AntProgress percent={value.SAST} status={_getProgressClass(value.SAST)} />
          </p>
          <p>
            <h6>DAST</h6>

            <AntProgress percent={value.DAST} status={_getProgressClass(value.DAST)} />
          </p>
          <p>
            <h6>PEN</h6>

            <AntProgress percent={value.PEN} status={_getProgressClass(value.PEN)} />
          </p>
          <p>
            <h6>Vulnerabilities</h6>

            <AntProgress percent={value.Vulnerabilities} status={_getProgressClass(value.Vulnerabilities)} />
          </p>
        </div>
      );
    case "enabled":
      const type = value ? "primary" : "danger";
      const text = value ? "ENABLED" : "DISABLED";
      return (
        <AntText type={type} bold>
          {text}
        </AntText>
      );
    //return value ? 'ENABLED' : 'DISABLED';
    case "integration_type":
      return (
        <AntRow type={"flex"} align={"center"} gutter={[10, 10]} justify={"start"}>
          <AntCol>
            <SvgIcon icon={value} style={{ height: "2.4rem", width: "2.4rem" }} />
          </AntCol>
          <AntCol>
            <AntText>{(value || "").toUpperCase()}</AntText>
          </AntCol>
        </AntRow>
      );
    case "integration_types":
      if (Array.isArray(value)) {
        return (
          <AntRow type={"flex"} justify={"start"} gutter={[20, 10]}>
            {value.map(integration => (
              <AntCol key={integration}>
                <SvgIcon icon={integration} style={{ height: "2.4rem", width: "2.4rem" }} />
              </AntCol>
            ))}
          </AntRow>
        );
      }
      return value;
    case "products":
    case "workspaces":
      if (Array.isArray(value)) {
        return value.join(", ");
      }
      return value;

    case "status":
      const statusText = (
        <span style={{ textTransform: "capitalize" }}>
          {value && typeof value === "string" ? value?.replace("_", " ")?.toLowerCase() : ""}
        </span>
      );
      switch (value) {
        case "success":
        case "OPEN":
        case "open":
        case "ACTIVE":
        case "PASSED":
        case "SUCCESS":
        case "Success":
        case "SUCCEEDED":
        case "healthy":
          return <AntBadge status="success" text={statusText} />;
        case "failure":
        case "CLOSED":
        case "closed":
        case "INACTIVE":
        case "FAILED":
        case "FAILURE":
        case "Failed":
        case "failed":
        case "Killed":
        case "Aborted":
        case "ABORTED":
        case "infrastructure_faill":
        case "UNSTABLE":
        case "error":
        case "canceled":
          return <AntBadge status="error" text={statusText} />;
        case "running":
        case "IN_PROGRESS":
          return <AntBadge status="processing" text={statusText} />;
        case "warning":
        case "unknown":
          return <AntBadge color="#ffad00" text={statusText} />;

        default:
          return <AntBadge status="default" text={statusText} />;
      }

    case "status_badge":
      switch (value.value) {
        case "success":
        case "OPEN":
        case "ACTIVE":
        case "NEW":
          return <AntBadge status="success" text={value.name} />;
        case "failure":
        case "CLOSED":
        case "INACTIVE":
          return <AntBadge status="error" text={value.name} />;
        case "running":
        case "IN_PROGRESS":
          return <AntBadge status="processing" text={value.name} />;
        default:
          return <AntBadge status="default" text={value.name} />;
      }
    case "successful":
      const tagColor = value ? "green" : "red";
      return <Tag color={tagColor}>{value ? "Passed" : "Failed"}</Tag>;
    case "operation":
      switch (value) {
        case "added":
          return <AntTag color={"green"}>Added</AntTag>;
        case "removed":
          return <AntTag color={"red"}>Removed</AntTag>;
        case "changed":
          return <AntTag color={"blue"}>Changed</AntTag>;
        default:
          return <AntTag>{value}</AntTag>;
      }
    case "user":
      return (
        value &&
        typeof value === "string" && (
          <AvatarWithText
            text={value.includes("UNASSIGNED") ? "UNASSIGNED" : value.includes("UNKNOWN") ? "UNKNOWN" : value}
          />
        )
      );
    case "users":
      return (
        <>
          {value && Array.isArray(value) && (
            <>
              {value.length === 0 && "UNASSIGNED"}
              {value.length === 1 && value.map(user => <AvatarWithText text={user} key={uuid()} />)}
              {value.length > 1 && <NameAvatarList names={value} classRequired={false} />}
            </>
          )}
        </>
      );
    case "time_utc_f1":
      if (!value) return null;
      return moment(value).utc().format(DateFormats.DAY);
    case "time_utc_f2":
      if (!value) return null;
      return moment(value).utc().format(DateFormats.DAY_TIME);
    case "time_utc_f3":
      if (!value) return null;
      return moment(value).utc().format("DD/MM/YYYY h:mm A");
    case "tags_withColor":
      if (value && typeof value === "string") {
        return (
          <AntTooltip key="string-value-type" title={value}>
            <Tag color={getColor(value)} style={{ margin: "5px" }}>
              {(renderTagText(value) || "").toUpperCase()}
            </Tag>
          </AntTooltip>
        );
      }
      return (
        <>
          {value &&
            Array.isArray(value) &&
            value.map((tag, index) => {
              const val = typeof tag === "string" ? tag : tag[Object.keys(tag)[0]];
              return (
                <AntTooltip key={index} title={val}>
                  <Tag color={getColor(val)} style={{ margin: "5px" }}>
                    {renderTagText(val)}
                  </Tag>
                </AntTooltip>
              );
            })}
        </>
      );
    default: {
      // Specical case, csv should be CSV
      if (value.toLowerCase() === "csv") {
        return "CSV";
      }

      return value;
    }
  }
}
