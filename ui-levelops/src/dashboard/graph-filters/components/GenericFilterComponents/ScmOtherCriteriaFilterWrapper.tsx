import { Form, Checkbox, Radio } from "antd";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { AntCheckboxGroup } from "../../../../shared-resources/components";
import React from "react";
import { scmOtherCriteriaOptions } from "../Constants";
interface OtherCriteriaProps {
  metadata: any;
  onFilterValueChange?: (value: any, type: any) => void;
  filterProps: LevelOpsFilter;
}

export const ScmOtherCriteriaWrapper: React.FC<OtherCriteriaProps> = (props: OtherCriteriaProps) => {
  const { onFilterValueChange, filterProps, metadata: metaData } = props;
  const { label, beKey, filterMetaData, allFilters } = filterProps;
  const approvalStatus = get(allFilters, [beKey], "");
  const linkedIssuesKey = get(allFilters, ["linked_issues_key"], "false") === "true";
  return (
    <>
      <Form.Item key={beKey} label={label}>
        <AntCheckboxGroup
          className="criteria-group"
          value={approvalStatus}
          options={scmOtherCriteriaOptions}
          onChange={(e: number) => onFilterValueChange?.(e, beKey)}
        />
        <Checkbox
          checked={linkedIssuesKey}
          onChange={(e: any) => onFilterValueChange?.(e.target.checked ? "true" : "false", "linked_issues_key")}>
          Linked Issues
        </Checkbox>

        {linkedIssuesKey &&
          <div className="mt-10 ml-20">
            <Radio.Group onChange={(e: any) => onFilterValueChange?.(e.target.value ? "true" : "false", "has_issue_keys")} value={get(allFilters, ["has_issue_keys"], "false") === "true"}>
              <Radio value={true}>PRs with linked issues</Radio>
              <br></br>
              <Radio value={false}>PRs without linked issues</Radio>
            </Radio.Group>
          </div>
        }

      </Form.Item>
    </>
  );
};
