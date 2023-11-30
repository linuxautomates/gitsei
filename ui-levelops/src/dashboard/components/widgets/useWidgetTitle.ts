import { GET_WIDGET_TITLE_INTERVAL } from "dashboard/constants/applications/names";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import { useState, useEffect } from "react";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidgetDataSelector } from "reduxConfigs/selectors/widgetAPISelector";
import { getTimeForTrellisProfile, unixUTCToDate } from "utils/dateUtils";

// hook for widget title
export const useWidgetTitle = (id: any, reportType: string) => {
  const [intervalValue, setIntervalValue] = useState<any>({
    resultTime: undefined,
    intervalValue: undefined
  });

  const widgetTitleInterval = get(widgetConstants, [reportType, GET_WIDGET_TITLE_INTERVAL], undefined);

  const widgetDataState = useParamSelector(getWidgetDataSelector, { widgetId: id });
  const orgUsersState = useParamSelector(getGenericRestAPISelector, {
    uri: "dev_productivity_score_report",
    method: "list",
    uuid: id
  });

  const ouScoreState = useParamSelector(getGenericRestAPISelector, {
    uri: "dev_productivity_org_unit_score_report",
    method: "list",
    uuid: id
  });

  useEffect(() => {
    const startTime = widgetDataState?.data?.[0]?.start_time
      ? widgetDataState?.data?.[0]?.start_time
      : ouScoreState?.data?.records?.[0]?.start_time
      ? ouScoreState?.data?.records?.[0]?.start_time
      : orgUsersState?.data?.records?.[0]?.start_time
      ? orgUsersState?.data?.records?.[0]?.start_time
      : null;
    const endTime = widgetDataState?.data?.[0]?.end_time
      ? widgetDataState?.data?.[0]?.end_time
      : ouScoreState?.data?.records?.[0]?.end_time
      ? ouScoreState?.data?.records?.[0]?.end_time
      : orgUsersState?.data?.records?.[0]?.end_time
      ? orgUsersState?.data?.records?.[0]?.end_time
      : null;

    let resultTime =
      widgetDataState?.data?.length > 0
        ? Math.max(...widgetDataState?.data?.map((o: any) => o.result_time), 0)
        : ouScoreState?.data?.records?.[0]?.result_time
        ? ouScoreState?.data?.records?.[0]?.result_time
        : orgUsersState?.data?.records?.[0]?.result_time
        ? orgUsersState?.data?.records?.[0]?.result_time
        : null;

    const value = `${startTime ? unixUTCToDate(startTime, "DD MMM YYYY") : startTime} to ${
      endTime ? unixUTCToDate(endTime, "DD MMM YYYY") : endTime
    }`;
    resultTime = resultTime ? getTimeForTrellisProfile(resultTime) : "";
    if (widgetTitleInterval && (intervalValue.resultTime !== resultTime || intervalValue?.intervalValue !== value)) {
      setIntervalValue({ intervalValue: startTime && endTime ? value : "", resultTime });
    }
  }, [widgetDataState, widgetDataState, ouScoreState, orgUsersState, id]);

  return intervalValue;
};
