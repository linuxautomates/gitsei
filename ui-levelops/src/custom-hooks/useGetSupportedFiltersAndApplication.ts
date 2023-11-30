import { useEffect, useState } from "react";
import widgetConstants from "../dashboard/constants/widgetConstants";
import { get } from "lodash";

export function useGetSupportedFiltersAndApplication(reportType: string | undefined) {
  const [supportedFilters, setSupportedFilters] = useState<any>();
  const [application, setApplication] = useState<string>();

  const getWidgetConstant = (data: any) => {
    return get(widgetConstants, [reportType, data], undefined);
  };

  useEffect(() => {
    if (reportType) {
      setSupportedFilters(getWidgetConstant("supported_filters"));
      setApplication(getWidgetConstant("application"));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reportType]);

  return [application, supportedFilters];
}
