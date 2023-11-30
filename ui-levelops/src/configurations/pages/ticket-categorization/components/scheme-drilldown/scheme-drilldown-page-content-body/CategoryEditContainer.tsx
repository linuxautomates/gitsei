import { notification } from "antd";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { TICKET_CATEGORIZATION_SCHEME } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";

import React, { useCallback, useEffect, useMemo } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { ticketCategorizationSchemeUpdate } from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { ticketCategorizationSchemesRestGetSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";

const CategoryEditContainer: React.FC<{ schemeId: string; categoryId: string }> = ({ schemeId, categoryId }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const restScheme: RestTicketCategorizationScheme = useParamSelector(ticketCategorizationSchemesRestGetSelector, {
    scheme_id: schemeId
  });

  const restUpdateState = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "update",
    uuid: schemeId
  });

  useEffect(() => {
    const { data } = restUpdateState || {};
    if (data?.changed === true) {
      notification.success({ message: "Scheme Updated Successfully" });
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "update", schemeId));
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "get", schemeId));
      history.goBack();
    }
  }, [restUpdateState]);

  const handleUpdate = useCallback(
    (updatedCategory: any) => {
      restScheme.addCategory(updatedCategory);
    },
    [restScheme]
  );

  const handleSave = useCallback(() => {
    dispatch(ticketCategorizationSchemeUpdate(restScheme));
  }, [restScheme]);

  const handleCancel = useCallback(() => {
    history.goBack();
  }, []);

  return (
    <></>
    // <CategoryEditCreateContainer
    //   restScheme={restScheme}
    //   categoryId={categoryId}
    //   handleUpdate={handleUpdate}
    //   handleSave={handleSave}
    //   handleCancel={handleCancel}
    // />
  );
};

export default CategoryEditContainer;
