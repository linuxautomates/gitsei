import { get, isEqual } from "lodash";
import { useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { epicPriorityList } from "reduxConfigs/actions/restapi";
import { ticketCategorizationSchemesList } from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";

export const TICKET_CATEGORIZATION_SCHEMES_LIST_STATE_ID = "TICKET_CATEGORIZATION_LIST_STATE_ID";
export const EPIC_LIST_STATE_ID = "EPIC_LIST_STATE_ID";

const useCategoryOrEpicAcross = (filters: any) => {
  const across = filters["across"];
  const [apiData, setApiData] = useState<any[]>([]);
  const [apiLoading, setApiLoading] = useState<boolean>(false);
  const dispatch = useDispatch();
  const acrossRef = useRef<string>();

  const schemesListState = useParamSelector(getGenericRestAPISelector, {
    uri: "ticket_categorization_scheme",
    method: "list",
    uuid: TICKET_CATEGORIZATION_SCHEMES_LIST_STATE_ID
  });

  const epicListState = useParamSelector(getGenericRestAPISelector, {
    uri: "epic_priority_report",
    method: "list",
    uuid: EPIC_LIST_STATE_ID
  });

  const forceRefresh = useMemo(() => {
    if (across === "epic") {
      const epicListData = get(epicListState, ["data"], {});
      if (Object.keys(epicListData).length > 0) {
        return false;
      }
      return true;
    } else {
      const schemeListData = get(schemesListState, ["data"], {});
      if (Object.keys(schemeListData).length > 0) {
        return false;
      }
      return true;
    }
  }, [across, schemesListState, epicListState]);

  const fetchData = () => {
    if (!forceRefresh && across !== "epic") {
      setApiLoading(true);
      setApiData([]);
      return;
    }
    // TODO not doing api call when across is epic temparory
    if (across === "epic") {
      // dispatch(epicPriorityList(filters, EPIC_LIST_STATE_ID));
    } else if (across === "ticket_category") {
      dispatch(ticketCategorizationSchemesList({}, TICKET_CATEGORIZATION_SCHEMES_LIST_STATE_ID));
      setApiLoading(true);
      setApiData([]);
    }
  };

  useEffect(() => {
    acrossRef.current = across;
    fetchData();
  }, []);

  useEffect(() => {
    if (!isEqual(acrossRef.current, across)) {
      acrossRef.current = across;
      fetchData();
    }
  }, [across]);

  useEffect(() => {
    if (apiLoading) {
      let restState = across === "epic" ? epicListState : schemesListState;
      const loading = get(restState, ["loading"], true);
      const error = get(restState, ["error"], true);
      if (!loading && !error) {
        const data = get(restState, ["data"], {});
        setApiData(get(data, ["records"], []));
        setApiLoading(false);
      } else if (!loading && error) {
        setApiLoading(false);
      }
    }
  }, [apiLoading, schemesListState, epicListState, across]);

  return { apiData, apiLoading };
};
export default useCategoryOrEpicAcross;
