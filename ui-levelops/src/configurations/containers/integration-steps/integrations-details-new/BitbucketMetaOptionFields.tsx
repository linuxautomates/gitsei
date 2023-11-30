import React from "react";
import { Form } from "antd";
import { AntCheckbox } from "shared-resources/components";

interface BitbucketMetaOptionFieldsProps {
  metadata: any;
  updateField: any;
}
const BitbucketMetaOptionFields: React.FC<BitbucketMetaOptionFieldsProps> = ({ updateField, metadata }) => {
  return (
    <>
      <Form.Item label={"Options"} colon={false} key={"options"}>
        <AntCheckbox
          checked={metadata?.fetch_commits || ""}
          onChange={(e: any) => updateField("fetch_commits", e.target.checked)}>
          Fetch Commits
        </AntCheckbox>
        <AntCheckbox
          checked={metadata?.fetch_prs || ""}
          onChange={(e: any) => updateField("fetch_prs", e.target.checked)}>
          Fetch PRs
        </AntCheckbox>
        <AntCheckbox
          checked={metadata?.fetch_pr_reviews || ""}
          onChange={(e: any) => updateField("fetch_pr_reviews", e.target.checked)}>
          Fetch PRs Reviews
        </AntCheckbox>
      </Form.Item>
      {metadata?.fetch_commits && (
        <Form.Item label={""} colon={false} key={"hidden"}>
          <AntCheckbox
            checked={metadata.fetch_commit_files}
            onChange={(e: any) => updateField("fetch_commit_files", e.target.checked)}>
            Fetch Commits Files
          </AntCheckbox>
        </Form.Item>
      )}
    </>
  );
};

export default BitbucketMetaOptionFields;
