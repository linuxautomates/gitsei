import { get } from "lodash";
import { FIELD_LIST_ENTITY } from "reduxConfigs/actions/restapi/fields-list.action";
import { createSelector } from "reselect";
import { selectedDashboardByIntegrationData } from "./integrationSelector";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getApplication = createParameterSelector((params: any) => params.application);

const _selectedDashboardFieldListData = createSelector(restapiState, (data: any) => {
  return get(data, FIELD_LIST_ENTITY, {
    data: {}
  });
});

export const selectedDashboardFieldsList = createSelector(
  _selectedDashboardFieldListData,
  (data: { data: Record<string, any[]> }) => {
    return data.data ?? {};
  }
);

export const fieldListDataSelector = createSelector(
  selectedDashboardByIntegrationData,
  selectedDashboardFieldsList,
  getApplication,
  (selectedIntegrations: any[], fieldsData: Record<string, any[]>, application: string): any[] => {
    if (selectedIntegrations.length) {
      const integrationIds = selectedIntegrations.map(integration => integration.id);
      const key = `${integrationIds.sort().join()}_${application}`;
      return (fieldsData[key] ?? []).map(
        (item: { field_key: string; field_type: string; name: string; field_id: number | string; title: string }) => ({
          key: item.field_key || `customfield_${item?.field_id?.toString()}`,
          type: item.field_type,
          name: item.name || item.title
        })
      );
    }
    return [];
  }
);
