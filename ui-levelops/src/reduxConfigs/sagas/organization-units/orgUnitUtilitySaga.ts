import { forEach, get } from "lodash";
import { ORGANIZATION_UNIT_NODE } from "configurations/pages/Organization/Constants";
import { all, call, put, select, takeLatest } from "redux-saga/effects";
import { ORGANIZATION_UNIT_UTILITY_SAGA } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { RestOrgUsersFilterService, RestOrgUsersSchemaService } from "services/restapi";
import { filterOptionConfig } from "configurations/configuration-types/OUTypes";
import { optionType } from "dashboard/dashboard-types/common-types";
import { paginationEffectSaga } from "../paginationSaga";
import { restapiState } from "../../selectors/restapiSelector";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

export function* orgUnitUtilityEffectSaga(action: { type: string; id: string }): any {
  const orgUsersSchemaService = new RestOrgUsersSchemaService() as any;
  const orgUsersFiltersService = new RestOrgUsersFilterService();
  try {
    const URI = "org_users";
    const METHOD = "list";
    const UUID = "org-unit-users";
    let filters: any = {
      page: 0,
      page_size: 100000
    };

    yield put(
      genericRestAPISet(
        {
          loading: true
        },
        ORGANIZATION_UNIT_NODE,
        action.id,
        "-1"
      )
    );

    yield call(paginationEffectSaga, {
      uri: URI,
      method: METHOD,
      filters,
      id: UUID,
      derive: false,
      deriveOnly: "all"
    });
    let usersState = yield select(restapiState);
    let tableData: any[] = [...get(usersState, [URI, METHOD, UUID, "data", "records"], [])];
    const metadata = get(usersState, [URI, METHOD, UUID, "data", "_metadata"], {});
    const totalPages = Math.ceil(metadata.total_count / filters.page_size);

    const apiCallArray: any[] = [];
    if (totalPages > 1) {
      for (let page = 1; page < totalPages; page++) {
        filters = {
          ...filters,
          page
        };
        apiCallArray.push({
          uri: URI,
          method: METHOD,
          filters,
          id: `${UUID}_${page}`,
          derive: false,
          deriveOnly: "all"
        });
      }

      yield all(apiCallArray.map((curCall: any) => call(paginationEffectSaga, curCall)));
      usersState = yield select(restapiState);
      apiCallArray.forEach((call: any) => {
        tableData = [...(tableData || []), ...get(usersState, [URI, METHOD, call.id, "data", "records"], [])];
      });
    }

    const userRecords = tableData.filter((user: any) => !!user?.email);

    const usersCustomAttributesState = yield call(orgUsersSchemaService.get);

    const customAttributesFieldsRecords = get(usersCustomAttributesState, ["data", "fields"], []);
    let exceptionKeys: string[] = ["full_name", "email"];

    const fields = (customAttributesFieldsRecords || []).map((customAttribute: any) => {
      let field = customAttribute?.key;
      if (!exceptionKeys.includes(field)) {
        field = `custom_field_${field}`;
      }
      return field;
    });

    const customAttributeOptionsState = yield call(orgUsersFiltersService.list, { fields: fields });
    const customAttributeOptionsRecords = get(customAttributeOptionsState, ["data", "records"], []);

    let customAttributeOptions: filterOptionConfig[] = [];

    forEach(customAttributesFieldsRecords, records => {
      let filterField = exceptionKeys?.includes(records?.key) ? records?.key : `custom_field_${records?.key}`;
      const fieldObj = customAttributeOptionsRecords.find(
        (caObject: any) => Object.keys(caObject || {})[0] === filterField
      );

      const corrRecords = get(Object.values(fieldObj || {})[0], ["records"], []);
      let transformedOptions: optionType[] = corrRecords.map((record: { key: string }) => ({
        label: record?.key || "",
        value: record?.key || ""
      }));

      customAttributeOptions.push({
        label: records?.display_name,
        value: filterField,
        options: transformedOptions || []
      });
    });

    yield put(
      genericRestAPISet(
        {
          users: userRecords,
          custom_attributes: customAttributeOptions,
          loading: false
        },
        ORGANIZATION_UNIT_NODE,
        action.id,
        "-1"
      )
    );
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.ORG_UNITS,
        data: { e, action }
      }
    });
  }
}

export function* orgUnitUtitlitySagaWatcher() {
  yield takeLatest(ORGANIZATION_UNIT_UTILITY_SAGA, orgUnitUtilityEffectSaga);
}
