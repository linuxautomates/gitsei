import React from "react";
import { Form } from "antd";
import { AntCheckbox } from "shared-resources/components";

interface GithubMetaOptionsProps {
  metadata: any;
  updateField: any;
}
const GitlabMetaOptions: React.FC<GithubMetaOptionsProps> = ({ updateField, metadata }) => {
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
          checked={metadata?.fetch_issues || ""}
          onChange={(e: any) => updateField("fetch_issues", e.target.checked)}>
          Fetch Issues
        </AntCheckbox>
        <AntCheckbox
          checked={metadata?.fetch_projects || ""}
          onChange={(e: any) => updateField("fetch_projects", e.target.checked)}>
          Fetch Projects
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

export default GitlabMetaOptions;
