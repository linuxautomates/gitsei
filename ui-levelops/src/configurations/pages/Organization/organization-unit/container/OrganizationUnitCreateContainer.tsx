import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps, useParams } from "react-router-dom";
import queryString from "query-string";
import { cloneDeep, concat, filter, get, map } from "lodash";
import { orgUnitBasicInfoType, orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { NEW_ORG_UNIT_ID, ORGANIZATION_UNIT_NODE, ORG_UNIT_UTILITIES } from "../../Constants";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  orgUnitCreateRestDataSelect,
  orgUnitCreateSuccessState
} from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { OrganizationUnitCreate, OrgUnitUtilities } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { WebRoutes } from "routes/WebRoutes";
import { tagsSelector } from "reduxConfigs/selectors/tags.selector";
import { restapiClear, tagsGetOrCreate } from "reduxConfigs/actions/restapi";
import OrgUnitEditCreateContainer from "./org-unit-edit-create-container/OrgUnitEditCreateContainer";
import { ProjectPathProps } from "classes/routeInterface";
import { putDevProdParentProfile } from "reduxConfigs/actions/devProdParentActions";
import { Spin } from "antd";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";

const OrganizationUnitCreateContainer: React.FC<RouteComponentProps> = ({ location, history }) => {
  const draftOrgUnit: RestOrganizationUnit = useParamSelector(orgUnitCreateRestDataSelect);
  const orgUnitCreateState: string[] = useParamSelector(orgUnitCreateSuccessState, { id: NEW_ORG_UNIT_ID });
  const dispatch = useDispatch();
  const tagsRestState = useSelector(tagsSelector);
  const [create_tags_loading, setCreateTagsLoading] = useState<boolean>(false);
  const tags: any[] = useMemo(() => draftOrgUnit?.tags || [], [draftOrgUnit]);
  const { ou_category_tab, ou_workspace_id } = queryString.parse(location.search);
  const projectParams = useParams<ProjectPathProps>();
  const [trellisProfileForUpdate, setTrellisProfileForUpdate] = useState<any>(undefined);
  const newTrellisProfile = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);

  const trellisProfileOUUpdateState = useParamSelector(getGenericUUIDSelector, {
    uri: "trellis_profile_ou",
    method: "update",
    uuid: "current_ou_profile"
  });
  useEffect(() => {
    if (orgUnitCreateState.length) {
      if (newTrellisProfile) {
        const orgOuToApply: any = cloneDeep(trellisProfileForUpdate?.target_ou_ref_ids);
        if (trellisProfileForUpdate.target_ou_ref_ids) {
          delete trellisProfileForUpdate.target_ou_ref_ids;
        }
        trellisProfileForUpdate.associated_ou_ref_ids = [orgUnitCreateState?.[0]?.toString()];
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
              routeForRedirect,
              showCreateMessage: true
            }
          )
        );
      } else {
        dispatch(genericRestAPISet({}, "organization_unit_management", "create", NEW_ORG_UNIT_ID));
        history.push(
          WebRoutes.organization_page.root(projectParams, ou_workspace_id as string, ou_category_tab as string)
        );
      }
    }
  }, [orgUnitCreateState, ou_category_tab, ou_workspace_id, trellisProfileForUpdate]);

  useEffect(() => {
    dispatch(OrgUnitUtilities(ORG_UNIT_UTILITIES));
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
        dispatch(OrganizationUnitCreate(NEW_ORG_UNIT_ID, !newTrellisProfile));
        setCreateTagsLoading(false);
      }
    }
  }, [tagsRestState, create_tags_loading, newTrellisProfile]);

  const handleOUChanges = useCallback(
    (key: orgUnitBasicInfoType, value: any) => {
      (draftOrgUnit as any)[key] = value;
      handleOUUpdate(draftOrgUnit.json);
    },
    [draftOrgUnit]
  );

  const handleOUUpdate = useCallback((updatedOU: orgUnitJSONType) => {
    dispatch(genericRestAPISet(updatedOU, ORGANIZATION_UNIT_NODE, "create", NEW_ORG_UNIT_ID));
  }, []);

  const handleCancel = useCallback(() => {
    dispatch(genericRestAPISet({}, "organization_unit_management", "create", NEW_ORG_UNIT_ID));
    history.push(WebRoutes.organization_page.root(projectParams, ou_workspace_id as string, ou_category_tab as string));
  }, [ou_category_tab, ou_workspace_id]);

  const handleSave = useCallback(
    (validation: any, trellisProfile: any) => {
      const newTags = tags.filter((tag: string) => tag.startsWith("create:"));
      if (newTags.length > 0) {
        setCreateTagsLoading(true);
        dispatch(tagsGetOrCreate(newTags));
      } else {
        dispatch(OrganizationUnitCreate(NEW_ORG_UNIT_ID, !newTrellisProfile));
      }

      if (newTrellisProfile) {
        setTrellisProfileForUpdate(trellisProfile);
      }
    },
    [draftOrgUnit, newTrellisProfile]
  );
  if (newTrellisProfile && trellisProfileOUUpdateState?.loading) {
    return (
      <div style={{ width: "100%", height: "100%", display: "flex", justifyContent: "center" }}>
        <Spin size="default" className="flex justify-center align-center" />
      </div>
    );
  }

  return (
    <OrgUnitEditCreateContainer
      orgUnit={draftOrgUnit}
      handleCancel={handleCancel}
      handleSave={handleSave}
      handleOUChanges={handleOUChanges}
    />
  );
};

export default OrganizationUnitCreateContainer;
