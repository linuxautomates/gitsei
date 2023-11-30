import React from "react";
import { Link } from "react-router-dom";
import { actionsColumn, tableCell, updatedAtColumn } from "utils/tableUtils";
import { RestWorkItem } from "classes/RestWorkItem";
import { AntText, NameAvatarList } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { statusColumn } from "dashboard/pages/dashboard-tickets/configs/common-table-columns";
import { getWorkitemDetailPage } from 'constants/routePaths';
import { getQnAProgress } from "../../utils/helper";

export const tableConfig = isCompact => [
  {
    ...baseColumnConfig("Issue", "vanity_id", { width: "8%" }),
    render: (item, record, index) => (
      <AntText>
        <Link to={`${getWorkitemDetailPage()}?workitem=${item}`}>{item}</Link>
      </AntText>
    ),
    span: 8
  },
  {
    ...baseColumnConfig("Assessment", "questionnaire_template_name"),
    filterType: "apiMultiSelect",
    filterField: "questionnaire_template_ids",
    uri: "questionnaires",
    searchField: "name",
    span: 8
  },
  {
    ...updatedAtColumn("created_at", "Created On", { useUTC: true }),
    filterType: "dateRange",
    filterLabel: "Created Between",
    filterField: "created_at",
    span: 8
  },
  {
    ...updatedAtColumn("updated_at", "Updated On", { useUTC: true }),
    filterType: "dateRange",
    filterLabel: "Updated Between",
    filterField: "updated_at",
    span: 8
  },
  {
    title: "Progress",
    key: "progress",
    dataIndex: "id",
    width: "8%",
    filterType: "select",
    ellipsis: true,
    filterField: "completed",
    options: [
      { label: "COMPLETED", value: true },
      { label: "NOT COMPLETED", value: false }
    ],
    render: (val, record) => tableCell("quiz_progress", getQnAProgress(record)),
    span: 8
  },
  { ...actionsColumn() },
  ...(() => {
    return !!isCompact
      ? []
      : [
          {
            ...baseColumnConfig("Sent to", "assignees", { width: "5%" }),
            render: value => <NameAvatarList names={value} classRequired={false} />,
            span: 8
          },
          {
            ...baseColumnConfig("Tags", "tag_ids"),
            filterTitle: "Tags",
            filterType: "apiMultiSelect",
            filterField: "tag_ids",
            uri: "tags",
            render: value => tableCell("tags", value),
            span: 8
          },
          statusColumn(),
          {
            ...baseColumnConfig("Risk Level", "priority", { width: 200 }),
            render: value => tableCell("priority", value),
            filterType: "select",
            filterField: "priority",
            options: RestWorkItem.RISKS,
            span: 8
          }
        ];
  })()
];
