import React, { useEffect, useMemo, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation } from "react-router-dom";
import queryParser from "query-string";
import { forEach } from "lodash";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { EDIT_PROFILE, ADD_PROFILE, NEW_SCHEME_ID } from "../constants/ticket-categorization.constants";
import { getBreadcumsForSchemePage } from "../helper/getBreadcumsForSchemePage";
import { WebRoutes } from "routes/WebRoutes";
import ProfileBasicInfoComponent from "../components/create-edit-categories/category-basic-info/ProfileBasicInfoComponent";
import { EIConfigurationTabs, ProfileBasicInfoType } from "../types/ticketCategorization.types";
import { tabsConfig } from "../constants-new/constants";
import "./profileCreateEditContainer.styles.scss";
import CategoriesConfigurationComponent from "../components/scheme-drilldown/CategoriesConfigurationComponent";
import AllocationGoalsContainer from "./AllocationGoalsContainer";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";
export interface ProfileEditCreateContainerProps {
  profile: RestTicketCategorizationScheme;
  handleUpdate: (updatedCategory: any) => void;
  handleSave: () => void;
}

const ProfileCreateEditContainer: React.FC<ProfileEditCreateContainerProps> = ({
  profile,
  handleSave,
  handleUpdate
}) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const history = useHistory();
  const pageState = useSelector(getPageSettingsSelector);
  const { tab_key } = queryParser.parse(location.search);
  const entEffortInvestment = useHasEntitlements(Entitlement.SETTING_EFFORT_INVESTMENT);
  const entEffortInvestmentCountExceed = useHasEntitlements(
    Entitlement.SETTING_EFFORT_INVESTMENT_PROFILE_COUNT_3,
    EntitlementCheckType.AND
  );

  const [createAccess, editAccess] = useConfigScreenPermissions();

  useEffect(() => {
    const oldReadonly = getRBACPermission(PermeableMetrics.EFFORT_INVESTMENT_READ_ONLY);
    const hasSaveAccess = window.isStandaloneApp ? !oldReadonly : profile?.id ? editAccess : createAccess;
    const settings = {
      title: profile?.id ? profile?.name || EDIT_PROFILE : ADD_PROFILE,
      action_buttons: {
        manage_cancel: {
          type: "default",
          label: "Cancel",
          hasClicked: false
        },
        manage_save: {
          type: "primary",
          label: "Save",
          hasClicked: false,
          disabled:
            !hasSaveAccess ||
            !(profile?.categories || []).length ||
            !profile?.name ||
            !entEffortInvestment ||
            entEffortInvestmentCountExceed,
          tooltip: !entEffortInvestment || entEffortInvestmentCountExceed ? TOOLTIP_ACTION_NOT_ALLOWED : ""
        }
      },
      bread_crumbs: getBreadcumsForSchemePage(
        profile?.id || "",
        profile?.id ? profile?.name || EDIT_PROFILE : ADD_PROFILE
      ),
      bread_crumbs_position: "before",
      withBackButton: true,
      showDivider: true,
      headerClassName: "new-profiles-configure-page"
    };
    dispatch(setPageSettings(location.pathname, settings));
  }, [profile]);

  useEffect(() => {
    return () => {
      dispatch(clearPageSettings(location.pathname));
    };
  }, []);

  useEffect(() => {
    if (pageState && Object.keys(pageState).length > 0) {
      const page = pageState?.[location.pathname];
      if (page && page.hasOwnProperty("action_buttons")) {
        if (page?.action_buttons?.manage_save && page?.action_buttons?.manage_save?.hasClicked === true) {
          dispatch(setPageButtonAction(location.pathname, "manage_save", { hasClicked: false }));
          handleSave();
        }
        if (page?.action_buttons?.manage_cancel && page?.action_buttons?.manage_cancel?.hasClicked === true) {
          dispatch(setPageButtonAction(location.pathname, "manage_cancel", { hasClicked: false }));
          history.push(WebRoutes.ticket_categorization.list());
        }
      }
    }
  }, [pageState]);

  const handleChanges = useCallback(
    (key: ProfileBasicInfoType, value: string | boolean | object) => {
      (profile as any)[key] = value;
      if (key === "issue_management_integration") {
        const categories = profile?.categories || [];
        forEach(categories, category => {
          category.filter = {};
        });
      }
      handleUpdate({ ...(profile?.json || {}), draft: true });
    },
    [profile, handleUpdate]
  );

  const handleChangePathName = (tabKey: string) => {
    history.replace(WebRoutes.ticket_categorization.scheme.edit(profile?.id || NEW_SCHEME_ID, tabKey));
  };

  const renderContent = useMemo(() => {
    switch (tab_key) {
      case EIConfigurationTabs.BASIC_INFO:
        return <ProfileBasicInfoComponent profile={profile} handleChanges={handleChanges} />;
      case EIConfigurationTabs.CATEGORIES:
        return (
          <div className="category-configuration-container">
            <CategoriesConfigurationComponent profileId={profile?.id || NEW_SCHEME_ID} handleUpdate={handleUpdate} />
          </div>
        );
      case EIConfigurationTabs.ALLOCATION_GOALS:
        return <AllocationGoalsContainer profile={profile} />;
      default:
        return <></>;
    }
  }, [tab_key, profile, handleChanges]);

  return (
    <div className="profile-edit-create-container">
      <div className="tabs">
        {tabsConfig.map(tab => (
          <div
            className={`tab ${tab.tab_key === tab_key ? "active-tab" : ""}`}
            key={tab.tab_key}
            onClick={(e: any) => handleChangePathName(tab.tab_key as string)}>
            {tab.label}
          </div>
        ))}
      </div>
      <div className="content">{renderContent}</div>
    </div>
  );
};

export default ProfileCreateEditContainer;
