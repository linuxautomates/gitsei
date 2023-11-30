import { valuesToFilters } from "dashboard/constants/constants";
import { baseColumnConfig } from "utils/base-table-config";
import { typeKey } from "./helper";
import { get } from "lodash";
import { toTitleCase } from "utils/stringUtils";
export const slaTableConfig = (application: string, projectOptions: any, typeOptions: any) => {
  return [
    baseColumnConfig("Integration", "integration_name", { width: 200 }),
    {
      title: "Project",
      dataIndex: "project",
      key: "project",
      filterType: "multiSelect",
      options: projectOptions?.map((project: any) => project.key),
      filterField: "projects",
      filterLabel: "Projects",
      span: 8
    },
    {
      title: "Type",
      dataIndex: typeKey(application),
      key: typeKey(application),
      filterType: "multiSelect",
      options: typeOptions.map((type: any) => type.key),
      filterField: get(valuesToFilters, [typeKey(application)], "issue_types"),
      filterLabel: toTitleCase(get(valuesToFilters, [typeKey(application)], "Issue Types")),
      span: 8
    },
    {
      title: "Response SLA",
      dataIndex: "resp_sla",
      key: "resp_sla",
      render: (item: any, record: any, index: any) => {
        const days = Math.round(item / (60 * 60 * 24));
        return `${days} day(s)`;
      },
      width: 200,
      align: "right"
    },
    {
      title: "Resolution SLA",
      dataIndex: "solve_sla",
      key: "solve_sla",
      render: (item: any, record: any, index: any) => {
        const days = Math.round(item / (60 * 60 * 24));
        return `${days} day(s)`;
      },
      width: 200,
      align: "right"
    }
  ];
};
