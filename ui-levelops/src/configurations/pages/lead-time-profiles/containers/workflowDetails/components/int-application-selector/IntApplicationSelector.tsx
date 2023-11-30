import { getIntegrationUrlMap } from "constants/integrations";
import React, { useEffect, useMemo, useState } from "react";
import { AntSelect, AntText, IntegrationIcon } from "shared-resources/components";
import { Select } from "antd";
import {
  AZURE_SPLIT_AND_JIRA_APPLICATIONS,
  CICDApplications,
  SCMApplications,
  getAzureApplication
} from "helper/integration.helper";
import { stringTransform } from "utils/stringUtils";
import "./IntApplicationSelector.scss";
import { AzureApplicationSubType, Integration } from "model/entities/Integration";
import { useAllIntegrationState } from "custom-hooks/useAllIntegrationState";
import { uniq } from "lodash";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

const { Option } = Select;

interface IntApplicationSelectorProps {
  value: string;
  onChange: (app: string) => void;
  integration_type?: string;
  supportedIntegrationTypes?: string[];
}

const IntApplicationSelector: React.FC<IntApplicationSelectorProps> = ({
  value,
  onChange,
  supportedIntegrationTypes,
  integration_type
}) => {
  const [loading, setLoading] = useState<boolean>(true);
  const { isLoading, integrations } = useAllIntegrationState();
  const [supportedApplication, setSupportedApplication] = useState<string[]>([]);

  const allowAzureNonSplitedIntegration = useHasEntitlements(
    Entitlement.ALLOW_AZURE_NON_SPLIT_INTEGRATION,
    EntitlementCheckType.AND
  );

  useEffect(() => {
    if (!isLoading && loading) {
      let supportedIntegrations: string[] = [];
      const applications = integrations.map((integration: Integration) => integration.application);
      if (!supportedIntegrationTypes) {
        supportedIntegrations = uniq([...AZURE_SPLIT_AND_JIRA_APPLICATIONS, ...SCMApplications, ...CICDApplications]);
      } else {
        if (supportedIntegrationTypes.includes("IM"))
          supportedIntegrations = [
            ...supportedIntegrations,
            ...AZURE_SPLIT_AND_JIRA_APPLICATIONS.filter(
              item => ![AzureApplicationSubType.PIPELINES, AzureApplicationSubType.REPOS].includes(item as any)
            )
          ];
        if (supportedIntegrationTypes.includes("SCM"))
          supportedIntegrations = [...supportedIntegrations, ...SCMApplications];
        if (supportedIntegrationTypes.includes("CICD"))
          supportedIntegrations = [...supportedIntegrations, ...CICDApplications];
      }
      if (supportedIntegrationTypes) {
        setSupportedApplication(supportedIntegrations);
      } else {
        let filteredApplication = supportedIntegrations?.filter((int: string) => applications.includes(int));
        if (applications.includes(IntegrationTypes.AZURE as string)) {
          filteredApplication = [
            ...filteredApplication,
            AzureApplicationSubType.BOARDS,
            IntegrationTypes.AZURE_NON_SPLITTED
          ];
          const scmSupport = integrations.find(
            (integration: Integration) =>
              integration.application === IntegrationTypes.AZURE && integration?.metadata?.subtype === "scm"
          );
          const cicdSupport = integrations.find(
            (integration: Integration) =>
              integration.application === IntegrationTypes.AZURE && integration?.metadata?.subtype === "cicd"
          );
          if (scmSupport) {
            filteredApplication = [...filteredApplication, AzureApplicationSubType.REPOS];
          }
          if (cicdSupport) {
            filteredApplication = [...filteredApplication, AzureApplicationSubType.PIPELINES];
          }
        }
        setSupportedApplication(filteredApplication?.sort() as string[]);
      }
      setLoading(false);
    }
  }, [integrations]);

  const applicaitons = Object.values(getIntegrationUrlMap());

  const getTitle = (appType: string) => {
    if (appType === IntegrationTypes.AZURE_NON_SPLITTED) {
      appType = IntegrationTypes.AZURE;
    }
    const intDetails: any = applicaitons.find((app: any) => app.application === appType);
    let lowercaseTitleString = intDetails?.hasOwnProperty("lowercaseTitleString")
      ? intDetails.lowercaseTitleString
      : true;
    let bypassTitleTransform = intDetails?.bypassTitleTransform;

    let title = intDetails?.title || appType;

    // Special case for csv, it needs to be CSV
    if (title.toLowerCase() === "csv") {
      bypassTitleTransform = true;
    }

    return (
      <div className="flex justify-flex-start align-center int-app-selector">
        {
          // @ts-ignore
          <IntegrationIcon className="icon" size="small" type={appType} />
        }
        {title && (
          <AntText ellipsis>
            {!bypassTitleTransform ? stringTransform(title, /[_ ]+/ as any, " ", lowercaseTitleString) : title}
          </AntText>
        )}
      </div>
    );
  };
  const _value = useMemo(() => {
    if (value === IntegrationTypes.AZURE && integration_type) {
      return getAzureApplication(integration_type as string);
    }
    return value;
  }, [integration_type, value]);
  return (
    <AntSelect className="int-app-selector" value={_value} onChange={onChange}>
      {!isLoading &&
        (supportedApplication || []).map((int: any) => {
          if (int === IntegrationTypes.AZURE_NON_SPLITTED) {
            return allowAzureNonSplitedIntegration ? <Option value={int}>{getTitle(int)}</Option> : "";
          } else {
            return <Option value={int}>{getTitle(int)}</Option>;
          }
        })}
    </AntSelect>
  );
};

export default IntApplicationSelector;
