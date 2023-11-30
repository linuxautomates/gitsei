import Form from "antd/lib/form";
import { AntCheckbox, AntSelect } from "../../../../shared-resources/components";
import { timezones } from "../../../../utils/timezones.utils";
import React from "react";
import { Input } from "antd";
import { CommaSelectTag } from "shared-resources/containers/generic-form-elements";
import { SONARQUBE_PROJECT_KEYS_DESC } from "../../../../constants/integration.description";
import GithubMetaOptions from "./GithubMetaOptionsFields";
import BitbucketMetaOptionFields from "./BitbucketMetaOptionFields";
import GitlabMetaOptions from "./GitlabMetaOptionsFields";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface IntegrationMetaFieldsProps {
  satellite: boolean;
  applicationType: string;
  metadata: any;
  updateMetaField: (data: any) => void;
}

const TIMEZONE_APPLICATION = [IntegrationTypes.JIRA, IntegrationTypes.CIRCLECI];

export const IntegrationMetaFields: React.FC<IntegrationMetaFieldsProps> = ({
  applicationType,
  metadata,
  updateMetaField,
  satellite
}) => {
  const updateField = (key: string, value: any) => {
    if (key === "fetch_commits" && !value && metadata.fetch_commit_files) {
      updateMetaField({
        fetch_commits: false,
        fetch_commit_files: false
      });
    } else {
      updateMetaField({ [key]: value });
    }
  };

  return (
    <>
      {TIMEZONE_APPLICATION.includes(applicationType as IntegrationTypes) && (
        <Form.Item label={"Timezone"} colon={false} key={"timezone"}>
          <AntSelect
            allowClear={true}
            defaultValue={metadata?.timezone?.toString() || null}
            onSelect={(value: any) => updateField("timezone", value)}
            options={timezones.map(timezone => ({ label: timezone.label, value: timezone.value }))}
          />
        </Form.Item>
      )}

      {applicationType === IntegrationTypes.JIRA && (
        <Form.Item label={"JQL"} colon={false} key={"jql"}>
          <Input
            disabled={satellite}
            defaultValue={metadata?.jql}
            onChange={e => updateField("jql", e.target.value)}
            style={{ marginTop: "0.5rem" }}
          />
        </Form.Item>
      )}

      {applicationType === IntegrationTypes.SONARQUBE && (
        <Form.Item label={"project keys"} colon={false} key={"project_keys"} extra={SONARQUBE_PROJECT_KEYS_DESC}>
          <CommaSelectTag
            value={metadata?.project_keys || ""}
            onChange={(value: string) => updateField("project_keys", value)}
          />
        </Form.Item>
      )}

      {applicationType === IntegrationTypes.GITHUB && (
        <GithubMetaOptions updateField={updateField} metadata={metadata} />
      )}
      {applicationType === IntegrationTypes.GITLAB && (
        <GitlabMetaOptions updateField={updateField} metadata={metadata} />
      )}

      {applicationType === IntegrationTypes.SPLUNK && (
        <Form.Item label={"Options"} colon={false} key={"options"}>
          <AntCheckbox
            checked={metadata?.ignore_server_cert || ""}
            onChange={(e: any) => updateField("ignore_server_cert", e.target.checked)}>
            Ignore Server Cert
          </AntCheckbox>
          <AntCheckbox
            checked={metadata?.is_splunk_cloud || ""}
            onChange={(e: any) => updateField("is_splunk_cloud", e.target.checked)}>
            Is Splunk Cloud
          </AntCheckbox>
        </Form.Item>
      )}
      {applicationType === IntegrationTypes.ZENDESK && (
        <Form.Item label={"Options"} colon={false} key={"options"}>
          <AntCheckbox
            checked={metadata?.jiralinks_enabled || ""}
            onChange={(e: any) => updateField("jiralinks_enabled", e.target.checked)}>
            Jiralinks
          </AntCheckbox>
        </Form.Item>
      )}
      {applicationType === IntegrationTypes.AZURE && (
        <Form.Item label={"Options"} colon={false} key={"options"}>
          <AntCheckbox
            checked={metadata?.comments || ""}
            onChange={(e: any) => updateField("comments", e.target.checked)}>
            Fetch Workitem Comments
          </AntCheckbox>
        </Form.Item>
      )}
      {[IntegrationTypes.BITBUCKET, IntegrationTypes.BITBUCKET_SERVER].includes(
        applicationType as IntegrationTypes
      ) && <BitbucketMetaOptionFields updateField={updateField} metadata={metadata} />}
    </>
  );
};
