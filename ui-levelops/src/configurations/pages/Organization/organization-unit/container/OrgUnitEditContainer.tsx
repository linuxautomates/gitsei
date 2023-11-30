import React, { useCallback, useEffect, useRef, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Spin } from "antd";
import { RouteComponentProps, useParams } from "react-router-dom";
import queryString from "query-string";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { orgUnitBasicInfoType, orgUnitJSONType, OUDashboardType } from "configurations/configuration-types/OUTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import {
  ORGANIZATION_UNIT_NODE,
  ORG_UNIT_ASSOCIATED_DASHBOARD_CHANGE_WARNING,
  ORG_UNIT_CATEGORY_PARENT_NODE_WARNING
} from "../../Constants";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  getSelectedOU,
  orgUnitGetRestDataSelect,
  orgUnitUpdateSelect
} from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { OrganizationUnitGet, OrganizationUnitUpdate } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { WebRoutes } from "routes/WebRoutes";
import { getBreadcumsForEditOUPage } from "configurations/pages/ticket-categorization/helper/getBreadCrumbsForEditOUpage";
import { tagsGetOrCreate } from "reduxConfigs/actions/restapi";
import { cloneDeep, concat, filter, get, isEqual, map } from "lodash";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { tagsSelector } from "reduxConfigs/selectors/tags.selector";
import OrgUnitEditCreateContainer from "./org-unit-edit-create-container/OrgUnitEditCreateContainer";
import OrgUnitValidationModalComponent from "../components/OUValidationModalComponent";
import { widgetsSelector } from "reduxConfigs/selectors/widgetSelector";
import widgetConstants from "dashboard/constants/widgetConstants";
import { sessionUserWorkspacesSelections } from "reduxConfigs/selectors/session_current_user.selector";
import LocalStoreService from "services/localStoreService";
import { USERROLES } from "routes/helper/constants";
import { ProjectPathProps } from "classes/routeInterface";
import { COLLECTION_IDENTIFIER } from "constants/localStorageKeys";
import { putDevProdParentProfile } from "reduxConfigs/actions/devProdParentActions";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";

const OrganizationUnitEditContainer: React.FC<RouteComponentProps> = ({ location, history }) => {
  const params = useParams();
  const [showValidationModal, setShowValidationModal] = useState<boolean>(false);
  const [warningMessage, setWarningMessage] = useState<string>(ORG_UNIT_CATEGORY_PARENT_NODE_WARNING);
  const { view_version, active_version, ou_category_tab, ou_workspace_id } = queryString.parse(location.search);
  const allowedWorkspaces: Record<string, string[]> = useSelector(sessionUserWorkspacesSelections);
  const selectedOUState = useSelector(getSelectedOU);
  const selectedDashboardWidgetsState = useSelector(widgetsSelector);
  const orgUnitId = (params as any).id;
  const activeVersionRef = useRef<string>("");
  const ls = new LocalStoreService();
  const userRole = ls.getUserRbac();
  const hasAccess = userRole?.toLowerCase() === USERROLES.ADMIN;
  const projectParams = useParams<ProjectPathProps>();
  const newTrellisProfile = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);

  const [trellisProfileForUpdate, setTrellisProfileForUpdate] = useState<any>(undefined);

  useEffect(() => {
    orgUnitId && localStorage.setItem(COLLECTION_IDENTIFIER, orgUnitId);
  }, [orgUnitId]);

  useEffect(() => {
    const allowedWorkspacesKeys = Object.keys(allowedWorkspaces);
    const allowedOUs: string[] = get(allowedWorkspaces, [ou_workspace_id as string], []);
    // TODO: sharath
    if (allowedWorkspacesKeys.length && !hasAccess) {
      if (!allowedWorkspacesKeys.includes(ou_workspace_id as string) || !allowedOUs.includes(orgUnitId as string)) {
        history.push(WebRoutes.organization_page.root(projectParams));
      }
    }
  }, []);

  const [create_tags_loading, setCreateTagsLoading] = useState<boolean>(false);

  const orgUnit: RestOrganizationUnit = useParamSelector(orgUnitGetRestDataSelect, {
    id: orgUnitId
  });

  const validationMetricRef = useRef<{
    parent_ref_id?: number;
    ou_group_id?: string;
    associated_dashboards?: Array<OUDashboardType>;
  }>({});

  const tagsRestState = useSelector(tagsSelector);

  const orgUnitUpdateState = useParamSelector(orgUnitUpdateSelect, {
    id: orgUnitId
  });

  const trellisProfileOUUpdateState = useParamSelector(getGenericUUIDSelector, {
    uri: "trellis_profile_ou",
    method: "update",
    uuid: "current_ou_profile"
  });

  const setHeaderRef = useRef<boolean>(false);
  const dispatch = useDispatch();

  const tags: any[] = useMemo(() => orgUnit?.tags || [], [orgUnit]);

  const fetchOrgUnit = (version?: string) => {
    dispatch(OrganizationUnitGet(orgUnitId, {})); // removed version based call for now.
  };

  useEffect(() => {
    if (orgUnit.id && !Object.keys(validationMetricRef.current).length) {
      validationMetricRef.current = {
        parent_ref_id: orgUnit?.parentId,
        ou_group_id: orgUnit?.ouGroupId,
        associated_dashboards: orgUnit?.dashboards
      };
    }
  }, [orgUnit]);

  useEffect(() => {
    const currentVersionState = `${view_version}@${active_version}`;
    if (activeVersionRef.current !== currentVersionState) {
      let version = active_version;
      if (activeVersionRef.current) {
        const prevVersions = activeVersionRef.current.split("@");
        const prevViewVersion = prevVersions[0];
        if (prevViewVersion !== view_version) {
          version = view_version;
        }
      }
      activeVersionRef.current = currentVersionState;
      fetchOrgUnit(version as string);
    }
  }, [view_version, active_version]);

  const removeAllWidgetsData = () => {
    const widgets = Object.values(selectedDashboardWidgetsState || {});
    if (widgets.length) {
      widgets?.forEach((widget: any) => {
        const uri = get(widgetConstants, [widget.type, "uri"], "");
        dispatch(restapiClear(uri, "list", "-1"));
      });
    }
  };

  useEffect(() => {
    if (orgUnitUpdateState === "ok") {
      dispatch(restapiClear("custom_ou", "list", `${orgUnit?.id}_ou`));
      if (selectedOUState?.id === orgUnit?.id) {
        removeAllWidgetsData();
      }

      if (newTrellisProfile) {
        const orgOuToApply: any = cloneDeep(trellisProfileForUpdate?.target_ou_ref_ids);
        if (trellisProfileForUpdate.target_ou_ref_ids) {
          delete trellisProfileForUpdate.target_ou_ref_ids;
        }
        const routeForRedirect = WebRoutes.organization_page.root(
          projectParams,
          ou_workspace_id as string,
          ou_category_tab as string
        );
        dispatch(
          putDevProdParentProfile(
            { parent_profile: trellisProfileForUpdate, target_ou_ref_ids: orgOuToApply },
            "current_ou_profile",
            {
              history,
              routeForRedirect
            }
          )
        );
      } else {
        history.push(
          WebRoutes.organization_page.root(projectParams, ou_workspace_id as string, ou_category_tab as string)
        );
      }
    }
  }, [orgUnitUpdateState, ou_category_tab, ou_workspace_id, trellisProfileForUpdate]);

  useEffect(() => {
    if (!setHeaderRef.current) {
      const settings = {
        title: `Edit Collection`,
        bread_crumbs: getBreadcumsForEditOUPage(projectParams),
        bread_crumbs_position: "before",
        showDivider: true
      };
      setHeaderRef.current = true;
      dispatch(setPageSettings(location.pathname, settings));
    }
    return () => {
      dispatch(clearPageSettings(location.pathname));
      dispatch(genericRestAPISet({}, "organization_unit_management", "get", "-1"));
      dispatch(genericRestAPISet("false", "organization_unit_management", "update", orgUnitId));
      dispatch(genericRestAPISet({}, "organization_unit_management", "org_unit_utilities", "-1"));
      dispatch(restapiClear("tags", "getOrCreate", "-1"));
      dispatch(restapiClear("tags", "list", "-1"));
    };
  }, []);

  useEffect(() => {
    if (create_tags_loading) {
      const loading = get(tagsRestState, ["getOrCreate", 0, "loading"], true);
      const error = get(tagsRestState, ["getOrCreate", 0, "error"], false);
      if (!loading && !error) {
        const newtags = get(tagsRestState, ["getOrCreate", 0, "data"], []);
        dispatch(restapiClear("tags", "getOrCreate", "-1"));
        const mappedTags = concat(
          filter(tags, (tag: any) => !tag?.includes("create:")),
          map(newtags, (tag: any) => tag.id)
        );
        handleOUChanges("tags", mappedTags);
        dispatch(OrganizationUnitUpdate(orgUnitId, !newTrellisProfile));
        setCreateTagsLoading(false);
      }
    }
  }, [tagsRestState, create_tags_loading, newTrellisProfile]);

  const handleOUChanges = useCallback(
    (key: orgUnitBasicInfoType, value: any) => {
      (orgUnit as any)[key] = value;
      handleOUUpdate(orgUnit.json);
    },
    [orgUnit]
  );

  const handleOUUpdate = useCallback(
    (updatedOU: orgUnitJSONType) => {
      dispatch(genericRestAPISet(updatedOU, ORGANIZATION_UNIT_NODE, "get", orgUnitId));
    },
    [orgUnitId]
  );

  const handleCancel = useCallback(() => {
    dispatch(genericRestAPISet({}, "organization_unit_management", "get", orgUnitId));
    history.push(WebRoutes.organization_page.root(projectParams, ou_workspace_id as string, ou_category_tab as string));
  }, [orgUnitId, ou_category_tab, ou_workspace_id]);

  const handleValidation = () => {
    validationMetricRef.current = {};
    handleValidationCancel();
    handleSave(true);
  };

  const handleValidationCancel = () => {
    setShowValidationModal(false);
  };

  const saveTagsAndOrg = () => {
    const newTags = tags.filter((tag: string) => tag.startsWith("create:"));
    if (newTags.length > 0) {
      setCreateTagsLoading(true);
      dispatch(tagsGetOrCreate(newTags));
    } else {
      dispatch(OrganizationUnitUpdate(orgUnitId, !newTrellisProfile));
    }
  };

  const handleSave = useCallback(
    (ignoreValidation?: boolean, trellisProfile?: any) => {
      if (!ignoreValidation && orgUnit.hasChilds) {
        const orgUnitCategoryOrParentChanged =
          !isEqual((orgUnit.parentId ?? "").toString(), (validationMetricRef.current.parent_ref_id ?? "").toString()) ||
          !isEqual(orgUnit.ouGroupId, validationMetricRef.current.ou_group_id);

        const orgUnitAssociatedDashboardsChange = !isEqual(
          orgUnit?.dashboards,
          validationMetricRef.current.associated_dashboards
        );

        if (orgUnitCategoryOrParentChanged || orgUnitAssociatedDashboardsChange) {
          const warningMessage = orgUnitCategoryOrParentChanged
            ? ORG_UNIT_CATEGORY_PARENT_NODE_WARNING
            : ORG_UNIT_ASSOCIATED_DASHBOARD_CHANGE_WARNING;
          setWarningMessage(warningMessage);
          setShowValidationModal(true);
        } else {
          saveTagsAndOrg();
        }
      } else {
        saveTagsAndOrg();
      }
      if (newTrellisProfile) {
        setTrellisProfileForUpdate(trellisProfile);
      }
    },
    [orgUnitId, orgUnit]
  );

  if (!orgUnit?.id || (newTrellisProfile && trellisProfileOUUpdateState?.loading)) {
    return (
      <div style={{ width: "100%", height: "100%", display: "flex", justifyContent: "center" }}>
        <Spin size="default" className="flex justify-center align-center" />
      </div>
    );
  }

  return (
    <>
      <OrgUnitValidationModalComponent
        handleCancel={handleValidationCancel}
        showValidationModal={showValidationModal}
        handleProceed={handleValidation}
        warningMessage={warningMessage}
      />
      <OrgUnitEditCreateContainer
        orgUnit={orgUnit}
        handleCancel={handleCancel}
        handleSave={handleSave}
        handleOUChanges={handleOUChanges}
      />
    </>
  );
};

export default OrganizationUnitEditContainer;
