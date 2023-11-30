import React, { useEffect, useMemo, useRef } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { ticketCategorizationSchemesRestGetSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
import { AntModalComponent as AntModal } from "shared-resources/components/ant-modal/ant-modal.component";
import AllocationGoalsContainer from "configurations/pages/ticket-categorization/containers-new/AllocationGoalsContainer";
import { ticketCategorizationSchemeUpdate } from "reduxConfigs/actions/restapi";
import { WebRoutes } from "routes/WebRoutes";
import { EIConfigurationTabs } from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import {
  DEFAULT_CATEGORY_DESCRIPTION,
  TICKET_CATEGORIZATION_SCHEME,
  UNCATEGORIZED_ID_SUFFIX
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { forEach, get, set } from "lodash";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { Spin } from "antd";

interface AlignmentModalProps {
  visible: boolean;
  profileId: string;
  reloadWidget: () => void;
  handleShowAlignmentModalToggle: (value: boolean) => void;
}

const AlignmentModal: React.FC<AlignmentModalProps> = ({
  visible,
  profileId,
  handleShowAlignmentModalToggle,
  reloadWidget
}) => {
  const profile: RestTicketCategorizationScheme = useParamSelector(ticketCategorizationSchemesRestGetSelector, {
    scheme_id: profileId
  });

  const profileGetStatus = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "get",
    uuid: profileId
  });

  const restUpdateState = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "update",
    uuid: profileId
  });

  const profileGetCheckRef = useRef<boolean>(false);

  useEffect(() => {
    if (!profileGetStatus?.loading && !!profile?.id && !profileGetCheckRef.current) {
      const newCategory = new RestTicketCategorizationCategory({ goals: profile?.goal ?? {} });
      newCategory.index = 0;
      newCategory.id = UNCATEGORIZED_ID_SUFFIX;
      newCategory.name = "Other";
      newCategory.description = DEFAULT_CATEGORY_DESCRIPTION;
      profile?.addCategory(newCategory.json);
      dispatch(genericRestAPISet(profile.json, TICKET_CATEGORIZATION_SCHEME, "get", profile?.id));
      profileGetCheckRef.current = true;
    }
  }, [profileGetStatus]);

  const history = useHistory();
  const dispatch = useDispatch();

  useEffect(() => {
    const { data } = restUpdateState || {};
    if (data?.changed === true && reloadWidget) {
      reloadWidget();
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "update", profileId));
    }
  }, [restUpdateState]);

  const handleSave = () => {
    const uncategorizedCategory = (profile?.categories || []).find(
      category => category?.id === UNCATEGORIZED_ID_SUFFIX
    );
    let profileJSON = profile?.json ?? {};
    if (uncategorizedCategory) {
      const curCategories = get(profileJSON, ["config", "categories"], {});
      let newUpdatedCategories: basicMappingType<any> = {};
      forEach(Object.keys(curCategories), key => {
        const value = curCategories?.[key];
        if (value?.id !== UNCATEGORIZED_ID_SUFFIX) {
          newUpdatedCategories[key] = value;
        }
      });
      set(profileJSON ?? {}, ["config", "categories"], newUpdatedCategories);
    }
    dispatch(ticketCategorizationSchemeUpdate(profileJSON));
    handleShowAlignmentModalToggle(false);
  };

  const handleEditProfile = () => {
    history.push(WebRoutes.ticket_categorization.scheme.edit(profileId, EIConfigurationTabs.BASIC_INFO));
  };

  const renderHeader = useMemo(() => {
    return (
      <div className="profile-title-container">
        <div className="profile-title">
          <h3>{profile?.name}</h3>
          <AntButton onClick={handleEditProfile}>Edit Profile</AntButton>
        </div>
        <p className="desc">{`Define ideal resource allocation ranges for categories in ${profile?.name}.`}</p>
      </div>
    );
  }, [profile?.name, handleEditProfile]);

  const renderFooter = useMemo(() => {
    return (
      <div className="footer-container">
        <AntButton onClick={(e: any) => handleShowAlignmentModalToggle(false)}>Cancel</AntButton>
        <AntButton type="primary" onClick={handleSave}>
          Save
        </AntButton>
      </div>
    );
  }, [handleShowAlignmentModalToggle]);

  if (profileGetStatus?.loading) {
    return (
      <div className="centered m-15">
        <Spin />
      </div>
    );
  }

  return (
    <AntModal
      visible={visible}
      title={renderHeader}
      footer={renderFooter}
      centered={true}
      className="alignment-goals-modal"
      onCancel={(e: any) => handleShowAlignmentModalToggle(false)}>
      <AllocationGoalsContainer profile={profile} hasCustomHeader={true} />
    </AntModal>
  );
};

export default AlignmentModal;
