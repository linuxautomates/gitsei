import { get, isEqual } from "lodash";
import { RestAPIMetadata } from "model/APIMetadata.type";
import { DependencyList, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { apiFilterAction } from "reduxConfigs/actions/restapi/apiFilter.action";
import { apiFilterSelector } from "reduxConfigs/selectors/apiFilter.selector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { ApiDropDownData, APIFilterConfigType } from "./../model/filters/levelopsFilters";
import { isSanitizedValue } from "./../utils/commonUtils";

export const useAPIFilter = (
  config: ApiDropDownData,
  otherConfig?: Record<string, any>,
  deps?: DependencyList
): APIFilterConfigType => { 
  const uri = config?.uri;
  const method = config?.method ?? "list";
  const integrationIds = config?.integration_ids ?? ["0"];
  const additionalFilter = config?.additionalFilter ?? {};
  const id = `${integrationIds.sort().join(",")}_${uri}_${config?.specialKey}_${JSON.stringify(additionalFilter)}`;
  const childComponentFilter = config?.childComponentFilter ?? {};
  const callApiOnParentValueChange = config?.callApiOnParentValueChange ?? false;
  const filters = config?.filters ?? {};

  const getPayload = useMemo(() => {
    const payload = config?.payload;
    if (payload) {
      if (typeof payload === "function") return payload({ integrationIds, additionalFilter, childComponentFilter, filters });
      return payload;
    }
    return { integration_ids: integrationIds, filter: { integration_ids: integrationIds, ...additionalFilter } };
  }, [config, additionalFilter, integrationIds]);

  const restState = useParamSelector(apiFilterSelector, { uri, method, id });

  const dispatch = useDispatch();

  const [data, setData] = useState<any[]>();
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<boolean | string>();
  const [metadata, setMetadata] = useState<RestAPIMetadata>();

  const integrationIdsRef = useRef<string[]>();
  const additionalFilterRef = useRef<any>();

  useEffect(() => {
    if (
      config &&
      (!isEqual(integrationIdsRef.current, integrationIds) || !isEqual(additionalFilterRef.current, additionalFilter))
    ) {
      integrationIdsRef.current = integrationIds;
      additionalFilterRef.current = additionalFilter;
      const data = get(restState, "data", undefined);
      if (!isSanitizedValue(data) || callApiOnParentValueChange) {
        dispatch(apiFilterAction({ uri, method, id, payload: getPayload }));
        setLoading(true);
        setError(undefined);
        setMetadata(undefined);
      }
    }
  }, [integrationIds, additionalFilter, callApiOnParentValueChange]);

  useEffect(() => {
    if (config) {
      const loading = get(restState, "loading");
      const error = get(restState, "error");
      const data = get(restState, "data");
      if (!loading && !error && data) {
        setData(data?.records?.records ?? data);
        setLoading(false);
        setError(undefined);
        setMetadata(new RestAPIMetadata(data?.records?._metadata ?? {}));
      } else if (!loading && error) {
        setData(undefined);
        setLoading(false);
        setError(error);
        setMetadata(undefined);
      }
    }
  }, [restState]);

  return { data, loading, error, metadata };
};
