import { Form, Input } from "antd";
import { get, unset } from "lodash";
import React, { useCallback, useMemo } from "react";
import { EMPTY_FIELD_WARNING, ERROR } from "constants/formWarnings";
import { SelectRestapi } from "shared-resources/helpers/index";
import { IntegrationMetaFields } from "../../../containers/integration-steps/integrations-details-new/integration-meta-fields.component";
import { tagsList } from "reduxConfigs/actions/restapi/index";
import JiraIntegrationExcludeFieldComponent from "configurations/containers/integration-steps/integrations-details-new/JiraIntegrationExcludeFieldComponent";
import { SENSITIVE_FIELDS_KEY } from "configurations/containers/integration-steps/integrations-details-new/integration.constants";
import AzureOrganizationFilter from "configurations/containers/integration-steps/integrations-details-new/AzureOrganizationFIlter";
import { BITBUCKET_REPO_DESC } from "constants/integration.description";
import { CommaSelectTag } from "shared-resources/containers/generic-form-elements";
import { isAzure, isJira } from "helper/integration.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface IntegrationDetailsFormProps {
  integration_form: any;
  updateField: any;
  tagsLoading: boolean;
}

const IntegrationDetailsForm: React.FC<IntegrationDetailsFormProps> = props => {
  const { integration_form, tagsLoading, updateField } = props;
  const fields = useMemo(() => {
    return [
      {
        id: "name",
        label: "Name",
        value: integration_form.name || "",
        required: true
      },
      {
        id: "description",
        label: "Description",
        value: integration_form.description || "",
        required: false
      }
    ];
  }, [integration_form]);

  const fieldStatus = useCallback(
    (field: any) => {
      return !get(integration_form, [field.id], false);
    },
    [integration_form]
  );

  const handleChange = useCallback(
    (value: any) => {
      value = value || [];
      updateField("tags", value);
    },
    [updateField]
  );

  const handleInputChange = useCallback(
    (id: string) => {
      return (e: any) => {
        updateField(id, e.target.value);
      };
    },
    [updateField]
  );

  const handleMetadataValueChange = useCallback(
    (value: any) => {
      let metadata = get(integration_form, ["metadata"], {});
      metadata = {
        ...metadata,
        ...value
      };
      if (Object.keys(metadata).includes(SENSITIVE_FIELDS_KEY) && !metadata[SENSITIVE_FIELDS_KEY]) {
        unset(metadata, [SENSITIVE_FIELDS_KEY]);
      }
      updateField("metadata", metadata);
    },
    [integration_form]
  );

  const getValueFromMetadata = (key: string, defaultValue = undefined) => {
    return get(integration_form, ["metadata", key], defaultValue);
  };

  return (
    <Form layout="vertical">
      <div className="integration-info-grid">
        {fields.map(field => {
          return (
            <Form.Item
              label={field.label}
              colon={false}
              key={field.id}
              required={field.required}
              validateStatus={field.required && fieldStatus(field) ? (field.value === "" ? ERROR : "") : ""}
              hasFeedback={field.required}
              help={field.required && fieldStatus(field) && field.value === "" && EMPTY_FIELD_WARNING}>
              <Input value={field.value} placeholder={field.label} onChange={handleInputChange(field.id)} />
            </Form.Item>
          );
        })}
        <Form.Item label={"Tags"} colon={false} key={"tags"}>
          <SelectRestapi
            value={integration_form.tags || []}
            mode={"multiple"}
            labelInValue={true}
            loading={tagsLoading}
            uri={"tags"}
            fetchData={tagsList}
            createOption={true}
            searchField="name"
            onChange={handleChange}
          />
        </Form.Item>
        {[IntegrationTypes.BITBUCKET, IntegrationTypes.BITBUCKET_SERVER].includes(integration_form.application) && (
          <Form.Item label={"REPOSITORIES"} colon={false} key={"bitbucket_repos"} help={BITBUCKET_REPO_DESC}>
            <CommaSelectTag
              onChange={(value: string) => handleMetadataValueChange({ repos: value })}
              value={integration_form?.metadata?.repos || ""}
            />
          </Form.Item>
        )}
        <IntegrationMetaFields
          updateMetaField={handleMetadataValueChange}
          applicationType={integration_form.application}
          metadata={integration_form?.metadata || {}}
          satellite={integration_form?.satellite || false}
        />
        {isJira(integration_form.application) && (
          <JiraIntegrationExcludeFieldComponent
            handleExcludeUpdate={handleMetadataValueChange}
            sensitiveFieldValue={getValueFromMetadata(SENSITIVE_FIELDS_KEY)}
          />
        )}
        {isAzure(integration_form.application) && (
          <AzureOrganizationFilter
            onOrganizationChange={handleMetadataValueChange}
            organizations={getValueFromMetadata("organizations")}
          />
        )}
      </div>
    </Form>
  );
};

export default React.memo(IntegrationDetailsForm);
