import { get } from "lodash";
import { DependencyList, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { genericGet, genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { genericApiState } from "reduxConfigs/selectors/generic.selector";

type genericApi = {
  id: string;
  uri: string;
  method: "list" | "get";
  data?: any;
  queryparams?: any;
};

export const useGenericApi = (api: genericApi, deps?: DependencyList) => {
  const dispatch = useDispatch();

  const restState = useSelector(state => genericApiState(state, api));

  const [apiData, setApiData] = useState<any>(api.method === "list" ? [] : {});
  const [apiLoading, setApiLoading] = useState<boolean>(false);

  const setDefaultData = () => {
    setApiData(api.method === "list" ? [] : {});
  };

  useEffect(
    () => {
      setDefaultData();
      if (api.method === "list") {
        dispatch(genericList(api.uri, api.method, api.data || {}, null, api.id, true, api.queryparams));
      } else {
        dispatch(genericGet(api.uri, api.id));
      }
      setApiLoading(true);
    },
    deps ? [...deps] : [] // eslint-disable-line react-hooks/exhaustive-deps
  );

  useEffect(() => {
    const loading = get(restState, ["loading"], true);
    const error = get(restState, ["error"], true);
    if (!loading && !error) {
      const data = get(restState, ["data"], {});
      if (api.method === "list") {
        setApiData(data.records || []);
      } else {
        setApiData(data);
      }
      setApiLoading(false);
    }

    if (!loading && error) {
      setDefaultData();
      setApiLoading(false);
    }
  }, [restState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    return () => {
      dispatch(restapiClear(api.uri, api.method, api.id));
      setDefaultData();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return [apiLoading, apiData];
};
