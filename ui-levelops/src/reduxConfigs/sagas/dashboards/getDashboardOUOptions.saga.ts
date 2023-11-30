import { call, select, takeLatest, put, take } from "redux-saga/effects";
import { cloneDeep, get } from "lodash";
import { GET_OU_OPTIONS } from "reduxConfigs/actions/actionTypes";
import { _dashboardsGetSelector } from "../../selectors/dashboardSelector";
import { restapiData, restapiError, restapiLoading } from "../../actions/restapi/restapiActions";
import { OrganizationUnitService } from "services/restapi/OrganizationUnit.services";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { OrganizationUnitRestGet, orgUnitPivotsList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getAllAncestors, getAllChildren, getTransformedOUs } from "./helper";
import { OU_OPTIONS_PIVOT_LIST, OU_OPTIONS_PIVOT_LIST_COMPLETE } from "./constants";
import queryString from "query-string";
import { getSelectedOU, orgUnitGetDataSelect } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";

export function* getDashboardOUSaga(action: any): any {
  const category = new OrganizationUnitService();
  yield put(restapiLoading(true, action.uri, action.method, action.uuid));
  try {
    const localWorkspaceId = queryString.parseUrl(window.location.href, {
      parseFragmentIdentifier: true
    })?.query?.workspace_id;
    //@ts-ignore
    const selectedWorkspace: any = yield select(getSelectedWorkspace);
    const workspaceID = localWorkspaceId ?? selectedWorkspace?.id;
    const OU =
      action?.OU ??
      queryString.parseUrl(window.location.href, { parseFragmentIdentifier: true })?.query?.OU ??
      queryString.parseUrl(window.location.href, { parseFragmentIdentifier: true })?.query?.ou_id;

    yield put(
      orgUnitPivotsList(
        OU_OPTIONS_PIVOT_LIST,
        { filter: { enabled: true, workspace_id: [workspaceID] } },
        OU_OPTIONS_PIVOT_LIST_COMPLETE
      )
    );
    yield take(OU_OPTIONS_PIVOT_LIST_COMPLETE);
    //@ts-ignore
    const pivotList = yield select(getGenericUUIDSelector, {
      uri: "pivots_list",
      method: "list",
      uuid: OU_OPTIONS_PIVOT_LIST
    });

    const pivotsIds = get(pivotList, ["data", "records"], []).map((pivot: any) => pivot.id);
    //@ts-ignore
    const ApiDataOU: any = yield call(category.list, { filter: { ou_category_id: pivotsIds } }, undefined);
    const transformedApiOUS = getTransformedOUs(get(ApiDataOU, ["data", "records"], []), []);
    const allRootOUs = transformedApiOUS.filter((ou: any) => !ou?.parent_ref_id);
    const filteredChildOUS = transformedApiOUS.filter((ou: any) => ou?.parent_ref_id);
    const OUOptions = allRootOUs.map((RootOu: any) => {
      const children = getAllChildren(RootOu, filteredChildOUS);
      if (children && Array.isArray(children) && children?.length) {
        RootOu.children = children.sort(stringSortingComparator());
      }
      return RootOu;
    });
    const selectedOUData = yield select(getSelectedOU);
    let selectedValue = [];
    if (Object.keys(selectedOUData || {}).length) {
      const ancestors = getAllAncestors([selectedOUData], transformedApiOUS, selectedOUData)?.reverse();
      selectedValue = ancestors;
    } else {
      const completeId = `${OU}_complete_id`;
      yield put(OrganizationUnitRestGet(OU, `${OU}_complete_id`));
      yield take(completeId);
      const selectedOuState = yield select(orgUnitGetDataSelect, { id: OU });
      const selectedOUData = get(selectedOuState, "data", {});
      const ancestors = getAllAncestors([selectedOUData], transformedApiOUS, selectedOUData)?.reverse();
      selectedValue = ancestors;
    }
    const allData = {
      treeStructureOptions: OUOptions,
      arrayStructureOptions: transformedApiOUS,
      selectedOptions: selectedValue
    };
    yield put(restapiData(allData, action.uri, action.method, action.uuid));
    yield put(restapiLoading(false, action.uri, action.method, action.uuid));
  } catch (e) {
    yield put(restapiError(e, action.uri, action.method, action.uuid));
    yield put(restapiData([], action.uri, action.method, action.uuid));
  }
}

export function* getDashboardOUSagaWatcher() {
  yield takeLatest(GET_OU_OPTIONS, getDashboardOUSaga);
}
