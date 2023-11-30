import { DependencyList, useEffect, useState } from "react";
import { getData, loadingStatus } from "../utils/loadingUtils";

export function useApiData(rest_api: any, apiName: string, apiMethod: string, id?: string, deps?: DependencyList) {
  const [apiData, setApiData] = useState<any>(null);
  const { loading: apiLoading, error: apiError } = loadingStatus(rest_api, apiName, apiMethod, id);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<any>(null);

  useEffect(() => {
    if (apiLoading && apiError) {
      setLoading(false);
      setError(apiError);
    }

    if (!apiLoading && apiError) {
      setLoading(true);
      if (apiData) {
        setApiData(null);
      }
    }

    if (!apiLoading && !apiError) {
      let data;
      if (apiMethod === "list") {
        data = getData(rest_api, apiName, apiMethod, id).records;
      } else {
        data = getData(rest_api, apiName, apiMethod, id);
      }
      setApiData(data);
      setLoading(false);
    }
  }, [apiLoading, apiError, deps]); // eslint-disable-line react-hooks/exhaustive-deps

  return [loading, apiData, error];
}
