import React, { useCallback, useEffect } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { notification } from "antd";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import {
  ticketCategorizationSchemeCreate,
  ticketCategorizationSchemeCreateUpdate
} from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  ticketCategorizationSchemesRestCreateSelector,
  _ticketCategorizationSchemesCreateSelector
} from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { WebRoutes } from "routes/WebRoutes";

import {
  DEFAULT_CATEGORY_DESCRIPTION,
  DEFAULT_CATEGORY_NAME,
  NEW_SCHEME_ID,
  PROFILE_CREATED_SUCCESSFULLY,
  TICKET_CATEGORIZATION_SCHEME,
  UNCATEGORIZED_ID_SUFFIX
} from "../../constants/ticket-categorization.constants";

import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import { RestTicketCategorizationProfileJSONType } from "../../types/ticketCategorization.types";
import ProfileCreateEditContainer from "../../containers-new/ProfileCreateEditContainer";
import { forEach, get, set, unset } from "lodash";
import { basicMappingType } from "dashboard/dashboard-types/common-types";

const CreateProfileContainer: React.FC = () => {
  const dispatch = useDispatch();
  const history = useHistory();

  const createProfileState = useParamSelector(_ticketCategorizationSchemesCreateSelector);
  const profile: RestTicketCategorizationScheme = useParamSelector(ticketCategorizationSchemesRestCreateSelector);
  const restCreateState = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "create",
    uuid: "0"
  });

  useEffect(() => {
    if (!createProfileState?.draft) {
      const newProfile: RestTicketCategorizationScheme = new RestTicketCategorizationScheme();
      const newCategory = new RestTicketCategorizationCategory({});
      newCategory.index = 0;
      newCategory.id = UNCATEGORIZED_ID_SUFFIX;
      newCategory.name = DEFAULT_CATEGORY_NAME;
      newCategory.description = DEFAULT_CATEGORY_DESCRIPTION;
      newProfile?.addCategory(newCategory.json);
      dispatch(genericRestAPISet(newProfile.json, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID));
    }
  }, []);

  useEffect(() => {
    const { data } = restCreateState || {};
    if (data?.id !== undefined) {
      notification.success({ message: PROFILE_CREATED_SUCCESSFULLY });
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID));
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "create", "0"));
      history.push(WebRoutes.ticket_categorization.list());
    }
  }, [restCreateState]);

  const handleUpdate = useCallback((updatedScheme: RestTicketCategorizationProfileJSONType) => {
    dispatch(genericRestAPISet(updatedScheme, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID));
  }, []);

  const handleSave = useCallback(() => {
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
    unset(profileJSON, ["config", "categoryColorMapping"]);
    dispatch(ticketCategorizationSchemeCreateUpdate(profileJSON, "create"));
  }, [profile]);

  return <ProfileCreateEditContainer profile={profile} handleUpdate={handleUpdate} handleSave={handleSave} />;
};

export default CreateProfileContainer;
