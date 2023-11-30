import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { changeCustomFieldPrefix } from "shared-resources/containers/dora-api-container/helper";

export const doraDrillDownTransformer = (drillDownData: any) => {
  const { drillDownProps, widget } = drillDownData;
  const { doraProfileIntegrationType, availableIntegrations, supportedCustomFields } = drillDownProps;
  const reportType = widget?.type || "";
  let finalFilters = {
      filter: {
        ...drillDownProps[drillDownProps.application],
        no_update_time_field: true,
        no_update_dashboard_time:true
      }
  };
  if(doraProfileIntegrationType === IM_ADO){
    finalFilters = changeCustomFieldPrefix(finalFilters, reportType);
   }
  return {filters:finalFilters};
};
