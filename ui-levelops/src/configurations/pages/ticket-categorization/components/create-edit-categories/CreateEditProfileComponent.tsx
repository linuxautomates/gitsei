import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { forEach } from "lodash";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useLocation } from "react-router-dom";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { ADD_PROFILE, EDIT_PROFILE, NEW_SCHEME_ID } from "../../constants/ticket-categorization.constants";
import { getBreadcumsForSchemePage } from "../../helper/getBreadcumsForSchemePage";
import { ProfileBasicInfoType } from "../../types/ticketCategorization.types";
import CategoriesConfigurationComponent from "../scheme-drilldown/CategoriesConfigurationComponent";
import "./CategoriesEditCreate.style.scss";
import ProfileBasicInfoComponent from "./category-basic-info/ProfileBasicInfoComponent";

export interface ProfileEditCreateContainerProps {
  profile: RestTicketCategorizationScheme;
  handleUpdate: (updatedCategory: any) => void;
  handleSave: () => void;
}

const ProfileEditCreateContainer: React.FC<ProfileEditCreateContainerProps> = ({
  profile,
  handleUpdate,
  handleSave
}) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const pageState = useSelector(getPageSettingsSelector);
  const setHeaderRef = useRef<boolean>(false);

  useEffect(() => {
    if (!setHeaderRef.current) {
      const settings = {
        title: (profile?.name || "").length ? EDIT_PROFILE : ADD_PROFILE,
        action_buttons: {
          manage_save: {
            type: "primary",
            label: "Save",
            hasClicked: false,
            disabled: !(profile?.categories || []).length || !profile?.name
          }
        },
        bread_crumbs: getBreadcumsForSchemePage(profile?.id || "", profile?.id ? EDIT_PROFILE : ADD_PROFILE),
        bread_crumbs_position: "before",
        withBackButton: true
      };
      setHeaderRef.current = true;
      dispatch(setPageSettings(location.pathname, settings));
    }
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
        if (page?.action_buttons?.manage_save) {
          dispatch(
            setPageButtonAction(location.pathname, "manage_save", {
              disabled: !(profile?.categories || []).length || !profile?.name
            })
          );
        }
      }
    }
  }, [profile]);

  useEffect(() => {
    if (pageState && Object.keys(pageState).length > 0) {
      const page = pageState?.[location.pathname];
      if (page && page.hasOwnProperty("action_buttons")) {
        if (page?.action_buttons?.manage_save && page?.action_buttons?.manage_save?.hasClicked === true) {
          dispatch(setPageButtonAction(location.pathname, "manage_save", { hasClicked: false }));
          handleSave();
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
    [profile]
  );

  return (
    <div className="category-modal-content-container">
      <ProfileBasicInfoComponent profile={profile} handleChanges={handleChanges} />
      <div className="category-configuration-container">
        <CategoriesConfigurationComponent profileId={profile?.id || NEW_SCHEME_ID} handleUpdate={handleUpdate} />
      </div>
    </div>
  );
};

export default ProfileEditCreateContainer;
