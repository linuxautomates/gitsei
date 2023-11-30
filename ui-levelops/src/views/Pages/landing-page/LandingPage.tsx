import { Spin } from "antd";
import { PivotType } from "configurations/configuration-types/OUTypes";
import { LANDING_PAGE_PIVOT_LIST_ID } from "configurations/pages/Organization/Constants";
import { cloneDeep, get, isEqual } from "lodash";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import queryString from "query-string";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { orgUnitPivotsList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntRow, AntCol, AntText, AntIcon } from "shared-resources/components";
import "./LandingPage.scss";
import LandingTreePage from "./LandingTreePage";
import { restapiClear, userProfileUpdate } from "reduxConfigs/actions/restapi";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";
import {
  getGenericWorkSpaceUUIDSelector,
  getSelectedWorkspace
} from "reduxConfigs/selectors/workspace/workspace.selector";
import { getWorkspaceCategories, workspaceApiClear } from "reduxConfigs/actions/workspaceActions";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import Tile from "shared-resources/components/tile/Tile";
import LocalStoreService from "services/localStoreService";
import { USERROLES } from "routes/helper/constants";
import { Organization_Routes, getBaseUrl } from "constants/routePaths";
import { CATEGORY_TILE_COLORS } from "./constant";
import { SelectDropdownKeys } from "core/containers/header/select-dropdowns/select-dropdown.types";
import { usePrevious } from "shared-resources/hooks/usePrevious";
import { sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { RestUsers } from "classes/RestUsers";
import { sessionCurrentUser } from "reduxConfigs/actions/sessionActions";
import { ProjectPathProps } from "classes/routeInterface";

interface LandingPageProps {}

const LandingPage: React.FC<LandingPageProps> = props => {
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();
  const ls = new LocalStoreService();
  const userRole = ls.getUserRbac();
  const hasManageAccess = userRole?.toLowerCase() === USERROLES.ADMIN;
  const params = queryString.parse(location.search) as any;
  const [pivotes, setPivotes] = useState<Array<PivotType>>([]);
  const [selectedPivot, setSelectedPivot] = useState<Record<string, any>>();
  const [pivotsLoading, setPivotsLoading] = useState<boolean>(false);
  const [pivotsCategoryLoading, setPivotsCatgoryLoading] = useState<boolean>(false);
  const getSelectedWorkspaceState = useSelector(getSelectedWorkspace);
  const prevSelectedWorkspaceID = usePrevious(getSelectedWorkspaceState?.id);
  const sessionCurrentUserData = useSelector(sessionCurrentUserState);
  const projectParams = useParams<ProjectPathProps>();

  const pivotsListState = useParamSelector(getGenericUUIDSelector, {
    uri: "pivots_list",
    method: "list",
    uuid: LANDING_PAGE_PIVOT_LIST_ID
  });

  const { workSpaceListData, loading } = useWorkSpaceList();
  const workspaceCategoriesState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method: "list",
    uuid: "workspace_categories"
  });

  useEffect(() => {
    /** directly selecting if only 1 pivot is available */
    if (pivotes?.length === 1 && !selectedPivot?.id) {
      pivotSelected(pivotes[0]);
    }
  }, [pivotes, selectedPivot]);

  useEffect(() => {
    if (params?.dashboard_id) {
      fetchWorkspaceCategories();
    }
    if (sessionCurrentUserData) {
      const sessionData = get(sessionCurrentUserData, ["data", "metadata", "selected_workspace"], undefined);
      // Updated session metadata of worksapce if not present.
      if (!sessionData) {
        const workspace = cloneDeep(getSelectedWorkspaceState);
        const sessionCurrentData = {
          ...sessionCurrentUserData,
          metadata: { ...sessionCurrentUserData?.metadata, selected_workspace: workspace }
        };
        const restUser = new RestUsers({
          ...sessionCurrentData
        });
        dispatch(userProfileUpdate(restUser));
        dispatch(sessionCurrentUser({ loading: false, error: false, data: sessionCurrentData }));
      }
    }
    return () => {
      dispatch(restapiClear("pivots_list", "list", LANDING_PAGE_PIVOT_LIST_ID));
      dispatch(workspaceApiClear("workspace_categories", "list"));
    };
  }, []);

  useEffect(() => {
    const pivot = queryString.parse(location.search);
    if (pivot) {
      setSelectedPivot({ id: pivot.ou_category_id, name: pivot.ou_group_name });
    }
  }, [location.search]);

  const fetchWorkspaceCategories = () => {
    setPivotsCatgoryLoading(true);
    dispatch(
      getWorkspaceCategories(
        get(getSelectedWorkspaceState, ["id"], "") as any,
        params?.dashboard_id,
        "workspace_categories"
      )
    );
  };

  const fetchData = (
    filters: any = sanitizeObjectCompletely({ filter: { workspace_id: [get(getSelectedWorkspaceState, ["id"])] } })
  ) => {
    setPivotes([]);
    setPivotsLoading(true);
    dispatch(orgUnitPivotsList(LANDING_PAGE_PIVOT_LIST_ID, { filter: { enabled: true, ...(filters?.filter || {}) } }));
  };

  useEffect(() => {
    if (!loading) {
      if (workSpaceListData?.length > 0) {
        dispatch(
          setPageSettings(location.pathname, {
            select_dropdown: {
              [SelectDropdownKeys.WORKSPACE_SELECT_DROPDOWN]: {
                workspaces: workSpaceListData || [],
                loading
              }
            }
          })
        );
      }
    }
  }, [workSpaceListData, loading]);

  useEffect(() => {
    if (
      !isEqual(prevSelectedWorkspaceID, getSelectedWorkspaceState?.id) &&
      params?.dashboard_id === undefined &&
      !pivotsCategoryLoading
    ) {
      fetchData();
    } else if (
      !isEqual(prevSelectedWorkspaceID, getSelectedWorkspaceState?.id) &&
      params?.dashboard_id &&
      !pivotsLoading
    ) {
      fetchWorkspaceCategories();
    }
  }, [getSelectedWorkspaceState, prevSelectedWorkspaceID]);

  useEffect(() => {
    if (pivotsLoading) {
      const loading = get(pivotsListState, ["loading"], true);
      const error = get(pivotsListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const records: Array<PivotType> = get(pivotsListState, ["data", "records"], []);
          setPivotes(records.sort((obj1: PivotType, obj2: PivotType) => obj1?.created_at - obj2?.created_at));
        }
        setPivotsLoading(false);
      }
    }
  }, [pivotsListState, pivotsLoading]);

  useEffect(() => {
    if (pivotsCategoryLoading) {
      const loading = get(workspaceCategoriesState, ["loading"], true);
      const error = get(workspaceCategoriesState, ["error"], true);
      if (!loading) {
        if (!error) {
          const records: Array<PivotType> = get(workspaceCategoriesState, ["data", "records"], []);
          setPivotes(records.sort((obj1: PivotType, obj2: PivotType) => obj1?.created_at - obj2?.created_at));
        }
        setPivotsCatgoryLoading(false);
      }
    }
  }, [workspaceCategoriesState, pivotsCategoryLoading]);

  const generateColor = (index: number) => {
    return `${CATEGORY_TILE_COLORS?.[index % CATEGORY_TILE_COLORS.length]}`;
  };

  const handleManage = (pivot: PivotType) => {
    history.push(
      `${getBaseUrl(projectParams)}${Organization_Routes._ROOT}?ou_workspace_id=${
        getSelectedWorkspaceState?.id
      }&ou_category_tab=${pivot.id}`
    );
  };

  const pivotSelected = (pivot: PivotType) => {
    const category = `?ou_category_id=${pivot?.id}`;
    history.push({
      search: params?.dashboard_id ? `${category}&dashboard_id=${params?.dashboard_id}` : `${category}`
    });
  };

  return (
    <>
      {pivotsLoading && (
        <div className="flex align-center justify-center" style={{ width: "100%", height: "100%" }}>
          <Spin />
        </div>
      )}
      {!pivotsLoading && selectedPivot?.id && <LandingTreePage selectedPivot={selectedPivot} />}
      {!pivotsLoading && !selectedPivot?.id && (
        <div className="landing-page-body">
          <AntRow className="nav-group">
            {pivotes.length > 0 &&
              pivotes.map((pivot, index) => {
                return (
                  <AntCol
                    className={"category"}
                    key={`${pivot?.name}-${index}`}
                    xs={12}
                    sm={12}
                    md={9}
                    lg={8}
                    xl={6}
                    span={9}>
                    <Tile
                      key={`${pivot?.name}-${index}`}
                      handleSelect={pivotSelected}
                      tile={pivot}
                      icon={{ name: "cluster", bgColor: generateColor(index) }}
                      hasManageAccess={hasManageAccess}
                      handleManage={() => handleManage(pivot)}
                    />
                  </AntCol>
                );
              })}
          </AntRow>
          {!pivotsLoading && !pivotsCategoryLoading && pivotes.length === 0 && (
            <div className="no-data">
              {pivotes.length === 0 && (
                <AntText className="mb-10">
                  {" "}
                  <AntIcon type="info-circle" /> {"No records found."}
                </AntText>
              )}
            </div>
          )}
        </div>
      )}
    </>
  );
};

export default LandingPage;
