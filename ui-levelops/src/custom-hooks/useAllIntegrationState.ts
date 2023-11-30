import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { findIntegrationType } from "helper/integration.helper";
import { isEmpty } from "lodash";
import { Integration } from "model/entities/Integration";
import { useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { getCachedIntegrations } from "reduxConfigs/actions/cachedIntegrationActions";
import { IntegrationState } from "reduxConfigs/reducers/integration/intergrationReducerUtils.types";
import { cachedIntegrationsState } from "reduxConfigs/selectors/CachedIntegrationSelector";

export const useAllIntegrationState = (
  integrationTypes: WorkflowIntegrationType[] = [],
  application: string = "",
  selectableIntList: string[] | undefined = undefined
) => {
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getCachedIntegrations("list"));
  }, []);

  const cachedIntegrations: IntegrationState = useSelector(cachedIntegrationsState);
  const { loading, error, data, hasAllIntegrations } = cachedIntegrations;

  const findIntegrationWithId = (id: string) => {
    if (hasAllIntegrations) {
      return data?.hasOwnProperty(id) ? data[id] : null;
    }
    return null;
  };

  const filteredIntegrations = useMemo(() => {
    if (hasAllIntegrations && !error && !loading) {
      let integrationList = Object.values(data ?? {});
      if (selectableIntList && selectableIntList.length > 0) {
        integrationList = integrationList.filter((int: Integration) => selectableIntList.includes(int.id.toString()));
      }
      integrationList = integrationList.filter((int: Integration) => {
        const intType = findIntegrationType(int);
        return intType && integrationTypes.includes(intType);
      });
      if (application) {
        if (application === IntegrationTypes.AZURE_NON_SPLITTED) {
          return integrationList.filter(
            (int: any) => int.application === IntegrationTypes.AZURE && !(int?.metadata).hasOwnProperty("subtype")
          );
        }
        return integrationList.filter((int: any) => int.application === application);
      }
      return integrationList;
    }
    return [];
  }, [data, hasAllIntegrations, error, loading, integrationTypes, application, selectableIntList]);

  return {
    isLoading: loading,
    integrations: hasAllIntegrations && !error && !loading ? Object.values(data ?? {}) : [],
    findIntegrationWithId,
    filteredIntegrations
  };
};
