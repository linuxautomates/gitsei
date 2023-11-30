import { Form } from "antd";
import React from "react";
import { AntSelect } from "shared-resources/components";
import { issueManagementSystemOptions } from "./DashboardFiltersConstants";

interface IssueManagementSystemFilterProps {
  disabled: boolean;
  filterValue: "azure_devops" | "jira";
  onMetadataChange?: (value: any, type: any, reportType?: String) => void;
}

const IssueManagementSystemFilter: React.FC<IssueManagementSystemFilterProps> = ({
  disabled,
  filterValue,
  onMetadataChange
}) => {
  return (
    <Form.Item key="support_system_select" label={"Issue Management System"} className="mt-2">
      <AntSelect
        showArrow={true}
        defaultValue={"Jira"}
        disabled={disabled}
        value={filterValue}
        options={issueManagementSystemOptions}
        mode={"single"}
        onChange={(value: any) => onMetadataChange?.(value, "issue_management_system")}
      />
    </Form.Item>
  );
};

export default IssueManagementSystemFilter;
