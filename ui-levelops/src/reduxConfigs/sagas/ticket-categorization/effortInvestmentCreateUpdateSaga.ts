import { notification } from "antd";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { RestTicketCategorizationProfileJSONType } from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import { SOMETHING_BAD_HAPPEN } from "constants/formWarnings";
import { get, map } from "lodash";
import { put, select, takeLatest, call } from "redux-saga/effects";
import { TICKET_CATEGORIZATION_CREATE_UPDATE } from "reduxConfigs/actions/actionTypes";
import { ticketCategorizationSchemeCreate, ticketCategorizationSchemeUpdate } from "reduxConfigs/actions/restapi";
import { INTEGRATION_CONFIG_ID_EFFORT_CATEGORIES } from "reduxConfigs/constants/effort-investment.constants";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { IssueManagementWorkItemFieldListService } from "services/restapi";
import { RestIntegrationsService } from "services/restapi/integrations";
import { updateEffortInvestmentProfile } from "../saga-helpers/BAEffortInvestment.helper";

function* effortInvestmentCreateUpdateSaga(action: {
  method: "create" | "update";
  data: RestTicketCategorizationProfileJSONType;
}): any {
  const { method, data } = action;

  const categoriesFieldState = yield select(getGenericUUIDSelector, {
    uri: "issue_management_workItem_Fields_list",
    method: "list",
    uuid: INTEGRATION_CONFIG_ID_EFFORT_CATEGORIES
  });

  let records: IntegrationTransformedCFTypes[] = get(categoriesFieldState, ["data", "records"], []);
  const isAzureType = data.config?.integration_type === "azure_devops";
  if (isAzureType && !records?.length) {
    const integrationService = new RestIntegrationsService();
    const integrationListState = yield call(integrationService.list, {
      filter: { applications: ["azure_devops"] },
      page_size: 100,
      page: 0
    });
    const integrationRecords: any[] = get(integrationListState, ["data", "records"], []);
    const integrationIds = map(integrationRecords, integrationObj => integrationObj.id);
    if (integrationIds.length) {
      const issueManageFieldsService = new IssueManagementWorkItemFieldListService();
      const response: { data: { records: any[] } } = yield call(issueManageFieldsService.list, {
        filter: { integration_ids: integrationIds, transformedCustomFieldData: true }
      });
      records = get(response, ["data", "records"], []);
    }
  }

  const transformedData = isAzureType ? updateEffortInvestmentProfile(data, records) : data;

  try {
    switch (method) {
      case "create":
        yield put(ticketCategorizationSchemeCreate(transformedData));
        break;
      case "update":
        yield put(ticketCategorizationSchemeUpdate(transformedData));
        break;
    }
  } catch (e) {
    notification.error({
      message: SOMETHING_BAD_HAPPEN
    });
  }
}

export function* effortInvestmentCreateUpdateSagaWatcher() {
  yield takeLatest(TICKET_CATEGORIZATION_CREATE_UPDATE as any, effortInvestmentCreateUpdateSaga);
}
