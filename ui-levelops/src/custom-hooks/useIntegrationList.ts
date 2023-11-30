import { DependencyList, useEffect, useState } from "react";

import { get } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { RestIntegrations } from "../classes/RestIntegrations";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import {
  cachedIntegrationsLoadingAndError,
  cachedIntegrationsListSelector
} from "reduxConfigs/selectors/CachedIntegrationSelector";
import { getCachedIntegrations } from "reduxConfigs/actions/cachedIntegrationActions";

type IntegrationList = {
  dashboardId: string;
};

export const useIntegrationList = (options: IntegrationList, deps?: DependencyList) => {
  const [integrationsLoading, setIntegrationsLoading] = useState<boolean>(false);
  const [integrationList, setIntegrationList] = useState<RestIntegrations[] | undefined>([]);

  const dispatch = useDispatch();
  const { dashboardId } = options;

  const dashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const integrationIds = get(dashboard, ["query", "integration_ids"], []);

  const integrationsLoadingErrorState = useSelector(cachedIntegrationsLoadingAndError);
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });

  useEffect(
    () => {
      if (!integrationIds.length) {
        return;
      }
      const integrationList = integrations;
      if (integrationList) {
        setIntegrationsLoading(false);
        setIntegrationList(integrationList);
      } else {
        loadIntegrations();
      }
    },
    deps ? [...deps] : []
  ); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (integrationsLoading) {
      const { loading, error } = integrationsLoadingErrorState;
      if (!loading && !error) {
        setIntegrationsLoading(false);
        setIntegrationList(integrations);
      }
    }
  }, [integrationsLoadingErrorState, integrations]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadIntegrations = () => {
    dispatch(getCachedIntegrations("list", undefined, integrationIds));
    setIntegrationsLoading(true);
    setIntegrationList(undefined);
  };

  useEffect(() => {
    return () => {
      dispatch(restapiClear("integrations", "list", "-1"));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return [integrationsLoading, integrationList];
};
