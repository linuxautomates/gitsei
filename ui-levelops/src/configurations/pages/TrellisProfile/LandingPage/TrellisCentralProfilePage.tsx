import { Spin } from "antd";
import React, { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { useHistory, useLocation } from "react-router-dom";
import { getBreadcrumbsForTrellisPageNew } from "../../ticket-categorization/helper/getBreadcumsForTrellisPage";
import { CANCEL_ACTION_KEY, getActionButtons, SAVE_ACTION_KEY } from "../../lead-time-profiles/helpers/helpers";
import "./TrellisCentralProfilePage.scss";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement } from "custom-hooks/constants";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericPageLocationSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { useHeader } from "../../../../custom-hooks/useHeader";
import TrellisJobRoles from "components/trelis-job-roles/TrellisJobRoles";
import { validateTrellisProfile } from "components/trelis-job-roles/TrelisJobRoleHelper";
import { getDevProdCentralProfile, putDevProdCentralParentProfile } from "reduxConfigs/actions/devProdParentActions";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { cloneDeep } from "lodash";
import { integrationsList, restapiLoading } from "reduxConfigs/actions/restapi";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";
import { OrgUnitUtilities } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { ORG_UNIT_UTILITIES } from "configurations/pages/Organization/Constants";
interface TrellisCentralProfilePageProps {
  isModalView?: boolean;
}

const TrellisCentralProfilePage: React.FC<TrellisCentralProfilePageProps> = ({}) => {
  const [trellisProfile, setTrellisProfile] = useState<any>(undefined);
  const entDevProdEdit = useHasEntitlements(Entitlement.SETTING_DEV_PRODUCTIVITY);
  const dispatch = useDispatch();
  const location = useLocation();
  const history = useHistory();
  const { setupHeader, onActionButtonClick } = useHeader(location.pathname);
  const trellisProfileCentralOu = useParamSelector(getGenericUUIDSelector, {
    uri: "trellis_profile_ou",
    method: "get",
    uuid: "central_profile"
  });
  const pagePathnameState = useParamSelector(getGenericPageLocationSelector, {
    location: location?.pathname
  });

  const [createAccess, editAccess] = useConfigScreenPermissions();

  useEffect(() => {
    dispatch(getDevProdCentralProfile({}, "central_profile"));
    dispatch(integrationsList({}));
    dispatch(OrgUnitUtilities(ORG_UNIT_UTILITIES));
  }, []);

  useEffect(() => {
    if (trellisProfileCentralOu?.data) {
      setTrellisProfile(cloneDeep(trellisProfileCentralOu?.data));
    }
  }, [trellisProfileCentralOu]);
  useEffect(() => {
    const oldReadonly = getRBACPermission(PermeableMetrics.WORKFLOW_PROFILE_READ_ONLY);
    const hasSaveAccess = window.isStandaloneApp ? !oldReadonly : editAccess || createAccess;
    const message: any = validateTrellisProfile(
      trellisProfile,
      true,
      false,
      !hasSaveAccess || !entDevProdEdit,
      true,
      true
    );

    if (pagePathnameState?.action_buttons?.manage_save?.tooltip !== message) {
      setupHeader({
        action_buttons: {
          action_cancel: {
            type: "secondary",
            label: "Cancel",
            hasClicked: false,
            disabled: false,
            showProgress: false
          },
          ...getActionButtons(!hasSaveAccess || !entDevProdEdit || message, message, "Save and Publish Changes")
        },
        showFullScreenBottomSeparator: true,
        title: "Trellis Central Profile",
        description: "",
        bread_crumbs: getBreadcrumbsForTrellisPageNew(),
        bread_crumbs_position: "before",
        headerClassName: "trellis-central-profile-wrapper"
      });
    }
  }, [trellisProfile]);
  useEffect(() => {
    onActionButtonClick((action: string) => {
      switch (action) {
        case CANCEL_ACTION_KEY:
          history.goBack();
          return {
            hasClicked: false
          };
        case SAVE_ACTION_KEY:
          dispatch(restapiLoading(true, "trellis_profile_ou", "get", "central_profile"));
          dispatch(putDevProdCentralParentProfile(trellisProfile, "central_profile"));
          return {
            hasClicked: false,
            disabled: true,
            showProgress: true
          };
        default:
          return null;
      }
    });
  }, [onActionButtonClick]);

  if (!trellisProfile || trellisProfileCentralOu?.loading) {
    return (
      <div style={{ width: "100%", height: "100%", display: "flex", justifyContent: "center" }}>
        <Spin size="default" className="flex justify-center align-center" />
      </div>
    );
  }

  return (
    <div className="trellis-central-profile">
      <TrellisJobRoles
        trellisProfile={trellisProfile}
        setTrellisProfile={setTrellisProfile}
        trellisProfileIsEnabled={true}
        setTrellisProfileIsEnabled={() => {}}
        showAddRole={true}
      />
    </div>
  );
};

export default TrellisCentralProfilePage;
