import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { getFiltereredIntegrationIds } from "dashboard/reports/dora/helper";
import { forEach, get, map, uniq, unset } from "lodash";
import { changeCustomFieldPrefix } from "shared-resources/containers/dora-api-container/helper";
import { genericDrilldownTransformer } from ".";

export const leadTimeDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { drillDownProps } = data;
  const { availableIntegrations, application } = drillDownProps;
  const availableApplications = uniq(map(availableIntegrations, (rec: any) => rec?.application));
  if (availableApplications.includes("jira") && availableApplications.includes("azure_devops")) {
    let newIntegartionIds: string[] = [];
    forEach(availableIntegrations, integration => {
      if (application?.includes(integration?.application)) {
        newIntegartionIds.push(integration?.id);
      }
    });
    filters = {
      ...filters,
      filter: {
        ...get(filters, ["filter"], {}),
        integration_ids: newIntegartionIds
      }
    };
  }
  return { acrossValue, filters };
};

export const leadTimeByTimeSpentInStagesDrilldownTransformer = (data: any) => {
  let { filters } = genericDrilldownTransformer(data);
  const { drillDownProps } = data;
  const { x_axis } = drillDownProps;

  filters = {
    ...filters,
    filter: {
      ...get(filters, ["filter"], {}),
      velocity_stages: x_axis !== 'Total_Time_In_stage' ? [x_axis] : ['$$ALL_STAGES$$']
    }
  };
  return { acrossValue: "none", filters };
};

const getNewDoraQuery = (integrationType:string) => {
  switch(integrationType){
    case "SCM":
      return {
        calculation:"pr_velocity",
        limit_to_only_applicable_data:false
      };
    case "IM":
      return {
        work_items_type:"jira",
        calculation:"ticket_velocity",
        limit_to_only_applicable_data:true
      };
    case IM_ADO:
      return {
        work_items_type:"work_item",
        calculation:"ticket_velocity",
        limit_to_only_applicable_data:true
      }
  }
}

export const DoraleadTimeMTTRDrilldownTransformer = (data: any) => {
  const { drillDownProps, widget} = data;
  const { doraProfileIntegrationType, availableIntegrations, supportedCustomFields } = drillDownProps;
  const reportType = widget?.type || "";
  const newQuery = getNewDoraQuery(doraProfileIntegrationType);
  const integrationIds = get(drillDownProps,[drillDownProps?.application,"integration_ids"],[]);
  let filteredIntegrationIds = integrationIds;
  const availableApplications: string[] = availableIntegrations.map((integration: any) => integration?.application);
  if(availableApplications?.includes("jira") && availableApplications?.includes("azure_devops") && availableIntegrations?.length > 0){
    filteredIntegrationIds = getFiltereredIntegrationIds(availableIntegrations,doraProfileIntegrationType, integrationIds);
  }
  let newFilters = {
    filter: {
      ...(drillDownProps?.[drillDownProps?.application] || {}),
      ...newQuery,
      integration_ids:filteredIntegrationIds
    }
  }
  if(doraProfileIntegrationType === "SCM" && newFilters?.filter?.hasOwnProperty("work_items_type")){
    unset(newFilters,["filter","work_items_type"]);
  }
  if(doraProfileIntegrationType === IM_ADO){
    newFilters = changeCustomFieldPrefix(newFilters, reportType);
   }
  return {
    filters: {
     ...newFilters
    },
    acrossValue:"values"
  };
};

export const leadTimeandMTTRTransformerDrilldownRecords = (data: { records: any[] }) => {
  if (!("records" in data)) return [];

  return data?.records.map((record: any) => {
    if (record?.data?.length) {
      record.data.forEach(({ key, mean }:any) => {
        record[`dynamic_column_aggs_${key}`] = mean;
      });
    }
    return record;
  });
};