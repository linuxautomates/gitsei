import {
  CATEGORY_DEFAULT_BACKGORUND_COLOR,
  TICKET_CATEGORIZATION_LIST_STATE_ID
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { RestTicketCategorizationProfileJSONType } from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import { getCategoryColorMapping } from "dashboard/pages/dashboard-drill-down-preview/helper";
import { get, isArray } from "lodash";
import { DependencyList, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { ticketCategorizationSchemesList } from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";

const URI = "ticket_categorization_scheme";
const API_METHOD = "list";

const useTicketCategorizationFilters = (reportType: string, deps?: DependencyList, profileId?: string) => {
  const dispatch = useDispatch();

  const ticketCategorizationListState = useParamSelector(getGenericRestAPISelector, {
    uri: URI,
    method: API_METHOD,
    uuid: TICKET_CATEGORIZATION_LIST_STATE_ID
  });

  const [apiData, setApiData] = useState<any>([]);
  const [apiLoading, setApiLoading] = useState<boolean>(false);

  const setDefaultData = () => {
    setApiData([]);
  };

  const categoryColorMapping = useMemo(() => {
    if (profileId && (apiData ?? []).length) {
      const profile = (apiData ?? []).find((cp: RestTicketCategorizationProfileJSONType) => cp?.id === profileId);
      let categoriesColorMapping = getCategoryColorMapping(get(profile ?? {}, ["config", "categories"], []));
      categoriesColorMapping["Other"] = get(
        profile,
        ["config", "uncategorized", "color"],
        CATEGORY_DEFAULT_BACKGORUND_COLOR
      );
      return categoriesColorMapping;
    }
    return {};
  }, [apiData, profileId]);

  useEffect(
    () => {
      // ! NEED TO CONFIRM
      // if (reportType !== "dev_profile" && !getWidgetConstant(reportType, SUPPORT_TICKET_CATEGORIZATION_FILTERS, false))
      //   return;
      const data = get(ticketCategorizationListState, ["data"], {});
      if (isArray(data?.records) && data?.records?.length) {
        setApiData(data.records || []);
      } else {
        setDefaultData();
        dispatch(ticketCategorizationSchemesList({}, TICKET_CATEGORIZATION_LIST_STATE_ID));
        setApiLoading(true);
      }
    },
    deps ? [...deps] : []
  );

  useEffect(() => {
    const loading = get(ticketCategorizationListState, ["loading"], true);
    const error = get(ticketCategorizationListState, ["error"], true);
    if (!loading && !error) {
      const data = get(ticketCategorizationListState, ["data"], {});
      setApiData(data.records || []);
      setApiLoading(false);
    }

    if (!loading && error) {
      setDefaultData();
      setApiLoading(false);
    }
  }, [ticketCategorizationListState]);

  useEffect(() => {
    return () => {
      // dispatch(restapiClear(URI, API_METHOD, TICKET_CATEGORIZATION_LIST_STATE_ID));
      setDefaultData();
    };
  }, []);

  return { apiLoading, apiData, categoryColorMapping };
};

export default useTicketCategorizationFilters;
