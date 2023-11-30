import { get } from "lodash";
import { DependencyList, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { getOUFiltersAction } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { RestDashboard } from "classes/RestDashboards";
import { getOuIntegrationsData } from "./helper";
import { IntegrationFilterType } from "dashboard/graph-filters/containers/Dora/typing";

export const useOrgUnitData = () => {
  const dispatch = useDispatch();
  const selectedOUState = useSelector(getSelectedOU);
  const sections = get(selectedOUState, "sections", []);
  const [allIntegrationData, setAllIntegrationData] = useState<IntegrationFilterType[]>([]);
  const dashboard: RestDashboard | null = useSelector(selectedDashboard);
  const integrationIds = useMemo(() => dashboard?.query?.integration_ids || [], [dashboard]);
  const dashboardIntegrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const [integrationLoading, setIntegrationLoading] = useState<boolean>(false);

  const ouUuid = `${selectedOUState?.id}_ou`;

  const OUIntegrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "custom_ou",
    method: "list",
    uuid: ouUuid
  });

  useEffect(() => {
    const integrationsData = getOuIntegrationsData(sections);
    const integrationFilters = get(OUIntegrationState, "data", []);
    if (!integrationFilters.length && integrationsData.length) {
      dispatch(getOUFiltersAction("custom_ou", "list", ouUuid, integrationsData));
      setIntegrationLoading(true);
    }
  }, [sections]);

  useEffect(() => {
    const loading = get(OUIntegrationState, "loading", true);
    const error = get(OUIntegrationState, "error", false);
    if (!loading && !error) {
      const integrationFilters = get(OUIntegrationState, "data", []);
      setAllIntegrationData(integrationFilters);
      setIntegrationLoading(false);
    }
  }, [OUIntegrationState]);

  return {
    data: allIntegrationData.length && !integrationLoading ? allIntegrationData : dashboardIntegrations,
    loading: integrationLoading
  };
};
