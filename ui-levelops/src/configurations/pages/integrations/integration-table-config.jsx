import { Col, Row, Tooltip } from "antd";
import { getIntegrationUrlMap } from "constants/integrations";
import { getIntegrationPage } from "constants/routePaths";
import moment from "moment";
import React from "react";
import { Link } from "react-router-dom";
import { AntText, IntegrationIcon } from "shared-resources/components";
import { actionsColumn, tableCell } from "utils/tableUtils";
import { stringSortingComparator } from "../../../dashboard/graph-filters/components/sort.helper";
import { stringTransform } from "../../../utils/stringUtils";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

export function applicationType() {
  const applicationOptions = [];
  const integrationMap = getIntegrationUrlMap();
  Object.keys(integrationMap).forEach(integration => {
    const _integration = integrationMap[integration];
    let install = true;
    if (_integration.hasOwnProperty("install")) {
      install = _integration.install;
    }
    if (install) {
      const _title = integrationMap[integration]?.title || integrationMap[integration]?.application;
      const application = _integration.application || integration;
      const lowercaseTitleString = integrationMap[integration]?.hasOwnProperty("lowercaseTitleString")
        ? integrationMap[integration].lowercaseTitleString
        : true;
      const bypassTitleTransform = integrationMap[integration]?.bypassTitleTransform;
      const title = !bypassTitleTransform ? stringTransform(_title, "_", " ", lowercaseTitleString) : _title;
      applicationOptions.push({
        label: title,
        value: application,
        key: title
      });
    }
  });
  applicationOptions.sort(stringSortingComparator());
  return applicationOptions;
}

function getApplicationName(application) {
  const allApplications = applicationType();
  return allApplications.find(app => app.value === application)?.label;
}

function nameWithTooltip(name) {
  return (
    <Tooltip title={name}>
      <AntText className={"pl-10"}>{name}</AntText>
    </Tooltip>
  );
}

const NameColumn = ({ item, url }) => {
  const [createAccess, hasAccess] = useConfigScreenPermissions();
  return (
    <AntText style={{ paddingLeft: "10px" }} className={"ellipsis"}>
      {window.isStandaloneApp || hasAccess ? <Link to={url}>{item}</Link> : item}
    </AntText>
  );
};

export const tableColumns = [
  {
    title: <span className={"pl-10"}>Name</span>,
    key: "name",
    dataIndex: "name",
    width: 150,
    ellipsis: true,
    render: (item, record, index) => {
      let _url = `${getIntegrationPage()}/edit?id=${record.id}`;
      return <NameColumn item={item} url={_url} />;
    }
  },
  {
    title: "Application",
    key: "application",
    dataIndex: "application",
    filterType: "multiSelect",
    options: applicationType(),
    span: 6,
    filterField: "applications",
    width: 100,
    ellipsis: true,
    render: (item, record, index) => {
      const iconType =
        item === "helix_core" || item === "helix"
          ? "perforce_helix_core"
          : item === "helix_swarm"
          ? "perforce_helix_swarm"
          : item;
      const application = getApplicationName(item)
        ? getApplicationName(item)
        : item?.split("_").join(" ").toUpperCase() || "";
      return (
        <Row gutter={[10, 10]} type={"flex"} align={"middle"} style={{ flexWrap: "nowrap", overflow: "hidden" }}>
          <Col>
            <IntegrationIcon type={iconType} />
          </Col>
          <Col className="ellipsis">
            <Tooltip title={application}>
              <AntText>{application}</AntText>
            </Tooltip>
          </Col>
        </Row>
      );
    }
  },
  {
    title: "Description",
    dataIndex: "description",
    key: "description",
    width: 150,
    ellipsis: true,
    render: (item, record, index) => {
      return nameWithTooltip(item);
    }
  },
  {
    title: "Status",
    key: "status",
    dataIndex: "status",
    width: 100,
    render: (item, record) => {
      if (!record.statusUpdated) {
        return "Loading...";
      }
      return tableCell("status", item);
    }
  },
  {
    title: "Satellite",
    key: "satellite",
    dataIndex: "satellite",
    width: 100,
    filterType: "binary",
    filterField: "satellite",
    span: 6,
    render: (item, record, index) => {
      if (item === true) {
        return <AntText className={"pl-10"}>YES</AntText>;
      }
      return null;
    }
  },
  {
    title: "Tags",
    filterTitle: "Tags",
    key: "tags",
    dataIndex: "tags",
    filterType: "apiMultiSelect",
    filterField: "tag_ids",
    uri: "tags",
    ellipsis: true,
    width: "20%",
    render: props => tableCell("tags", props),
    span: 6
  },
  {
    title: "Updated At",
    dataIndex: "updated_at",
    key: "updated_at",
    width: 120,
    render: (item, record, index) => {
      return <AntText>{moment.unix(item).format("MMM DD, YYYY")}</AntText>;
    }
  },
  {
    ...actionsColumn(),
    align: ""
  }
];
