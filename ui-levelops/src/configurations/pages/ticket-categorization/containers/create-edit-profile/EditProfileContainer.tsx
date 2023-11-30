import React, { useCallback, useEffect, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import {
  ticketCategorizationSchemeCreateUpdate,
  ticketCategorizationSchemeGet,
  ticketCategorizationSchemeUpdate
} from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { ticketCategorizationSchemesRestGetSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { useHistory } from "react-router-dom";
import { notification } from "antd";
import Loader from "components/Loader/Loader";
import {
  DEFAULT_CATEGORY_DESCRIPTION,
  DEFAULT_CATEGORY_NAME,
  PROFILE_UPDATED_SUCCESSFULLY,
  TICKET_CATEGORIZATION_SCHEME,
  UNCATEGORIZED_ID_SUFFIX
} from "../../constants/ticket-categorization.constants";
import { QueryStringType } from "../../types/ticketCategorization.types";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import { WebRoutes } from "routes/WebRoutes";
import { forEach, get, set, unset } from "lodash";
import ProfileCreateEditContainer from "../../containers-new/ProfileCreateEditContainer";
import { basicMappingType } from "dashboard/dashboard-types/common-types";

interface EditProfileContainerProps {
  id: string;
  edit: QueryStringType;
}

const EditProfileContainer: React.FC<EditProfileContainerProps> = ({ id, edit }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const [editMode, setEditMode] = useState<boolean>(edit === "true");
  const profile: RestTicketCategorizationScheme = useParamSelector(ticketCategorizationSchemesRestGetSelector, {
    scheme_id: id
  });

  const goBackRef = useRef<boolean | undefined>(undefined);
  const placeholderRef = useRef<boolean | undefined>(undefined);
  const profileGetCheckRef = useRef<boolean>(false);

  const restUpdateState = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "update",
    uuid: id
  });

  const profileGetStatus = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "get",
    uuid: id
  });

  useEffect(() => {
    const draftStatus = get(profileGetStatus, ["data", "draft"], false);
    if (!draftStatus) {
      dispatch(ticketCategorizationSchemeGet(id));
    }
  }, []);

  useEffect(() => {
    if (!profileGetStatus?.loading && !profileGetCheckRef.current && !!profile?.id) {
      const newCategory = new RestTicketCategorizationCategory({ goals: profile?.goal ?? {} });
      newCategory.index = 0;
      newCategory.id = UNCATEGORIZED_ID_SUFFIX;
      newCategory.name = DEFAULT_CATEGORY_NAME;
      newCategory.description = DEFAULT_CATEGORY_DESCRIPTION;
      profile?.addCategory(newCategory.json);
      dispatch(genericRestAPISet(profile.json, TICKET_CATEGORIZATION_SCHEME, "get", profile?.id));
      profileGetCheckRef.current = true;
    }
  }, [profileGetStatus]);

  useEffect(() => {
    const { data } = restUpdateState || {};
    if (data?.changed === true) {
      notification.success({ message: PROFILE_UPDATED_SUCCESSFULLY });
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "update", id));
      if (goBackRef.current !== false) {
        dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "get", id));
        history.push(WebRoutes.ticket_categorization.list());
      } else if (editMode) {
        goBackRef.current = true;
        setEditMode(false);
      }
    }
  }, [restUpdateState, goBackRef.current]);

  const handleUpdate = useCallback(
    (updatedScheme: any) => {
      dispatch(genericRestAPISet({ ...updatedScheme, draft: true }, TICKET_CATEGORIZATION_SCHEME, "get", id));
    },
    [id]
  );

  const handleSave = (goBack?: boolean) => {
    if (goBack !== undefined) {
      goBackRef.current = goBack;
    }
    if (placeholderRef.current !== undefined) {
      goBackRef.current = placeholderRef.current;
      placeholderRef.current = undefined;
    }
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
    dispatch(ticketCategorizationSchemeCreateUpdate(profileJSON, "update"));
  };

  if (profileGetStatus?.loading) return <Loader />;

  return <ProfileCreateEditContainer profile={profile} handleUpdate={handleUpdate} handleSave={handleSave} />;
};

export default EditProfileContainer;
