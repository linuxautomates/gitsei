import React, { useState, useEffect } from "react";

import { IngestionStatPayload } from "../ingestion-stats/ingestion-stats";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { IngestionCard } from "..";
import { notification } from "antd";

interface IngestionStatProps extends IngestionStatPayload {
  id: string;
}

const IngestionStatComponent: React.FC<IngestionStatProps> = ({ id, title, request, dataTransformer, filters }) => {
  const dispatch = useDispatch();

  const [loadingData, setLoadingData] = useState<boolean>(true);
  const [value, setValue] = useState<number | string>("-");

  const state = useParamSelector(getGenericRestAPISelector, { ...request });

  useEffect(() => {
    const loading = get(state, "loading", true);
    const error = get(state, "error", true);

    if (!loading) {
      setLoadingData(false);
      if (!error) {
        const data = get(state, "data", {});
        const value = dataTransformer(data);
        setValue(value);
      } else {
        notification.error({ message: "Error fetching stat - " + title });
      }
    }
  }, [state]);

  useEffect(() => {
    dispatch(genericList(request.uri, request.method, filters, null, request.uuid));
  }, []);

  return <IngestionCard title={title} value={value} loading={loadingData} />;
};

export default React.memo(IngestionStatComponent);
