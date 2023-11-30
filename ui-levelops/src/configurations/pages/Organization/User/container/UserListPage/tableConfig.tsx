import React, { ReactNode } from "react";
import { Button, Dropdown, Menu, Tooltip } from "antd";
import { uniq, get, forEach } from "lodash";
import { timeColumn } from "dashboard/pages/dashboard-tickets/configs/common-table-columns";
import { toTitleCase } from "utils/stringUtils";
import { baseColumnConfig } from "utils/base-table-config";
import {
  AntIcon,
  AntTag,
  AntText,
  AntTooltip,
  IntegrationIcon,
  SvgIcon,
  TooltipWithTruncatedText
} from "shared-resources/components";
import { actionsColumn, tableCell } from "utils/tableUtils";
import { managersConfigType, orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { WebRoutes } from "routes/WebRoutes";
import { ORG_UNIT_LIST_DASHBOARD_COUNT_COLUMN_INFO } from "configurations/pages/Organization/Constants";
import { Link, useParams } from "react-router-dom";
import { ProjectPathProps } from "classes/routeInterface";

const dynamicColumnPrefix = "dynamic_column_aggs_";

const getIntegrationPopoverContent = (data: any, showUserId = false) => (
  <Menu>
    {(data || []).map((item: any) => (
      <Menu.Item className="user-integration-container">
        {`${toTitleCase(item.application || "")} - ${toTitleCase(item.name)}${showUserId ? " - " + item?.user_id : ""}`}{" "}
      </Menu.Item>
    ))}
  </Menu>
);

export const renderUserDynamicColumns = (key: string, activeKey?: string) => {
  if (!key.includes(dynamicColumnPrefix)) {
    return;
  }
  const updatedKey = key.replace(dynamicColumnPrefix, "");

  return {
    ...baseColumnConfig(updatedKey, key),
    width: 150,
    render: (item: any, record: any, index: number) => {
      const value = typeof item === "string" ? item : item?.value;
      switch (item?.type) {
        case "date":
          return tableCell("updated_on", value);
        default:
          return <AntText>{`${value || ""}`}</AntText>;
      }
    }
  };
};
const ScoreCardLink = ({ user_id, name }: { user_id: string; name?: ReactNode }) => {
  const projectParams = useParams<ProjectPathProps>();
  const url = WebRoutes.dashboard.scorecard(projectParams, user_id);
  return (
    <Link to={url} target="_blank">
      {name}
    </Link>
  );
};
export const tableColumns: any[] = [
  {
    title: <span className={"pl-10"}>Name</span>,
    key: "full_name",
    dataIndex: "full_name",
    filterType: "multiSelect",
    filterField: "full_name",
    filterLabel: "Names",
    options: [],
    valueName: "full_name",
    width: 225,
    span: 8,
    fixed: "left",
    render: (item: any, record: any, index: number) => {
      if (record?.org_uuid) {
        return (
          <ScoreCardLink
            user_id={record?.org_uuid}
            name={
              <TooltipWithTruncatedText
                allowedTextLength={15}
                title={record?.full_name}
                textClassName={"org-name-label"}
              />
            }
          />
        );
      }

      return <TooltipWithTruncatedText allowedTextLength={15} title={record?.full_name} />;
    }
  },
  {
    title: "Email",
    dataIndex: "email",
    key: "email",
    filterType: "multiSelect",
    options: [],
    filterField: "email",
    filterLabel: "Emails",
    valueName: "email",
    width: 185,
    span: 8,
    ellipses: true,
    textWrap: "word-break",
    render: (item: any, record: any, index: number) => {
      if (record?.email) {
        return <TooltipWithTruncatedText allowedTextLength={15} title={record?.email} />;
      }
      return (
        <AntText className={"flex pl-10"}>
          <SvgIcon className={"pr-10"} icon="warning" /> Missing Email ID
        </AntText>
      );
    }
  },
  timeColumn("Last Updated At", "updated_at", { width: 160, sorter: false }),
  timeColumn("Created At", "created_at", { width: 120, sorter: false }),
  {
    title: "Integrations",
    dataIndex: "integration_user_ids",
    key: "integration_user_ids",
    width: 150,
    render: (item: any, record: any, index: any) => {
      if (Array.isArray(item)) {
        if (!!item.length) {
          const applications = uniq(item.map((integ: any) => integ.application));
          return (
            <div style={{ display: "flex", flexDirection: "row" }}>
              {applications.map((application: any) => {
                const integrations = item.filter((obj: any) => obj.application === application);
                return (
                  <Dropdown
                    overlay={getIntegrationPopoverContent(integrations, true)}
                    trigger={["hover"]}
                    className="integration-popup-trigger">
                    <div style={{ display: "flex", flexWrap: "wrap" }}>
                      <div key={application} className="application-icon-container">
                        {
                          // @ts-ignore
                          <IntegrationIcon
                            className="applications-menu-container-label-div__icon"
                            size="small"
                            type={application || ""}
                          />
                        }
                      </div>
                    </div>
                  </Dropdown>
                );
              })}
            </div>
          );
        } else {
          return "-";
        }
      }
      return "-";
    }
    // filterType: "multiSelect",
    // filterField: "user_type",
    // options: RestUsers.TYPES.map(option => ({ label: option, value: option }))
  },
  { ...actionsColumn(), align: "right", width: 100 }
];

export const orgUnitTableConfig = [
  {
    title: <span className={"pl-10"}>Name</span>,
    key: "name",
    dataIndex: "name",
    width: "15%",
    ellipsis: true,
    filterType: "apiMultiSelect",
    id: "name",
    uri: "organization_unit_filter_values",
    dropdownClassName: "org-dropdown-menu",
    filterField: "name",
    transformOptions: (list: any[]) => {
      return (list || []).map(obj => ({ name: obj?.key, id: obj?.key }));
    },
    hasNewRecordsFormat: true
  },
  {
    title: "Description",
    dataIndex: "description",
    key: "description",
    width: "15%"
  },
  {
    title: "Path",
    dataIndex: "path",
    key: "path",
    width: "10%",
    render: (item: string) => {
      if (!item) return "";
      const nPath = item?.substring(0, item.lastIndexOf("/"));
      return !!nPath ? nPath : "root collection";
    }
  },
  {
    title: (
      <div className="flex justify-center align-center">
        <AntText>{"# of Insights"}</AntText>
        <AntTooltip title={ORG_UNIT_LIST_DASHBOARD_COUNT_COLUMN_INFO}>
          <AntIcon type="info-circle" className="ml-5" style={{ color: "var(--title)" }} />
        </AntTooltip>
      </div>
    ),
    dataIndex: "no_of_dashboards",
    key: "no_of_dashboards",
    width: "8%",
    hidden: true
  },
  {
    title: "Managers",
    dataIndex: "managers",
    key: "managers",
    render: (item: managersConfigType[]) => {
      return (
        <div>
          {(item || []).map((manager, index) => (
            <span>{`${index > 0 ? "," : ""} ${manager.email}`}</span>
          ))}
        </div>
      );
    },
    width: "10%"
  },
  {
    title: "Associated Profiles",
    dataIndex: "name",
    key: "trellis_profile_name",
    width: "15%",
    ellipsis: true
  },
  {
    title: "Tags",
    dataIndex: "tags",
    key: "type",
    render: (item: any) => {
      if (Array.isArray(item)) {
        if (item.length === 0) {
          return "";
        }
        let result = item.map((val, index) => {
          if (val && val.length > 15) {
            val = val.substring(0, 15) + "...";
          }
          return <AntTag key={index}>{val}</AntTag>;
        });

        return <Tooltip title={item.join(", ")}>{result}</Tooltip>;
      }
      return item;
    },
    width: "10%",
    hidden: true
  },
  {
    title: "Integrations",
    dataIndex: "sections",
    key: "sections",
    width: "7%",
    render: (item: any) => {
      let integrations: { application: string; name: string }[] = [];
      forEach(item || [], section => {
        const integrationValues = Object.values(section?.integrations || {});
        const integration: string = integrationValues.length > 0 ? get(integrationValues[0], ["type"], "") : "";
        const integrationName: string = integrationValues.length > 0 ? get(integrationValues[0], ["name"], "") : "";
        integrations.push({ application: integration, name: integrationName });
      });

      if (Array.isArray(integrations)) {
        if (!!item.length) {
          return (
            <div style={{ display: "flex", flexWrap: "wrap" }}>
              {integrations.map(integration => (
                <Dropdown
                  overlay={getIntegrationPopoverContent(integrations)}
                  trigger={["hover"]}
                  className="integration-popup-trigger">
                  <div style={{ display: "flex", flexWrap: "wrap", marginRight: "10px" }}>
                    {!!integration.application && (
                      <div key={integration.application} className="application-icon-container">
                        {
                          // @ts-ignore
                          <IntegrationIcon
                            className="applications-menu-container-label-div__icon"
                            size="small"
                            type={integration.application || ""}
                          />
                        }
                      </div>
                    )}
                  </div>
                </Dropdown>
              ))}
            </div>
          );
        } else {
          return "";
        }
      }
      return "";
    }
  }
];

export const renderAssociatedProfileColumns =
  (onAssociateClick: (profileType: "trellis" | "workflow", org: orgUnitJSONType) => void) =>
  (item: any, record: any, index: any) => {
    return (
      <>
        {record.trellis_profile_name && record.trellis_profile_id ? (
          <>
            <AntText style={{ color: "#7E7E7E" }}>Trellis: </AntText>
            <AntText style={{ color: "#2967DD" }}>
              <Link to={WebRoutes.trellis_profile.scheme.edit(record.trellis_profile_id)}>
                {record.trellis_profile_name}
              </Link>
            </AntText>
          </>
        ) : (
          <Button type="link" style={{ padding: "0px" }} onClick={() => onAssociateClick("trellis", record)}>
            Associate Trellis Profile
          </Button>
        )}

        <br></br>
        {record.workflow_profile_name && record.workflow_profile_id ? (
          <>
            <AntText style={{ color: "#7E7E7E" }}>Workflow: </AntText>
            <AntText style={{ color: "#2967DD" }}>
              <Link to={WebRoutes.velocity_profile.scheme.edit(record.workflow_profile_id)}>
                {record.workflow_profile_name}
              </Link>
            </AntText>
          </>
        ) : (
          <Button type="link" style={{ padding: "0px" }} onClick={() => onAssociateClick("workflow", record)}>
            Associate Workflow Profile
          </Button>
        )}
      </>
    );
  };
