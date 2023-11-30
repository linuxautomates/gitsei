import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { useAllIntegrationState } from "custom-hooks/useAllIntegrationState";
import { Integration } from "model/entities/Integration";
import React from "react";
import { CustomSelect } from "shared-resources/components";

interface IntegrationFilterSelectorProps {
  integration: string[] | undefined;
  setIntegration: (integrationId: string[], integrationObject: Integration[]) => void;
  integrationType?: WorkflowIntegrationType[];
  application?: string;
}

const IntegrationFilterSelector: React.FC<IntegrationFilterSelectorProps> = ({
  integration,
  setIntegration,
  integrationType = [WorkflowIntegrationType.IM, WorkflowIntegrationType.SCM, WorkflowIntegrationType.CICD],
  application
}) => {
  const { isLoading, filteredIntegrations } = useAllIntegrationState(integrationType, application);

  const onChangeHandler = (intId: string[]) => {
    if (intId.length) {
      const selectedIntegrations = filteredIntegrations.filter((integration: Integration) =>
        intId.includes(integration.id as string)
      );
      if (selectedIntegrations) setIntegration(intId, selectedIntegrations);
    }
  };

  return (
    <CustomSelect
      valueKey="id"
      labelKey="name"
      labelCase="none"
      style={{ width: 400 }}
      showArrow={true}
      createOption={false}
      sortOptions
      mode="multiple"
      options={filteredIntegrations}
      placeholder="Select Integration"
      value={integration?.map((item: any) => item?.toString())}
      onChange={onChangeHandler}
      loading={isLoading}
      maxTagCount={2}
    />
  );
};

export default IntegrationFilterSelector;
