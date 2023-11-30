import React, { useEffect, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import queryString from "query-string";
import { VelocityConfigCreate, VelocityConfigEdit } from "../../components";
import "./velocity-configs-create-edit.style.scss";
import Loader from "../../../../../components/Loader/Loader";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  VELOCITY_CONFIG_LIST_ID_DEFAULT,
  velocityConfigsListDataSelector
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { velocityConfigsList } from "reduxConfigs/actions/restapi/velocityConfigs.actions";
import { useDispatch } from "react-redux";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { notification } from "antd";
import { getBaseUrl, VELOCITY_CONFIGS_ROUTES } from "../../../../../constants/routePaths";

const VelocityConfigsCreateEditPage: React.FC = props => {
  const location = useLocation();
  const dispatch = useDispatch();
  const history = useHistory();

  let configId: string = (queryString.parse(location.search).configId as string) || "new";
  const stageName: string = queryString.parse(location.search).stageName as string;
  const isNew = configId === "new";
  const isDefault = configId === "default";

  const configListState = useParamSelector(velocityConfigsListDataSelector, {
    id: VELOCITY_CONFIG_LIST_ID_DEFAULT
  });

  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    if (isDefault) {
      setLoading(true);
      dispatch(
        velocityConfigsList(
          {
            filter: {
              default: true
            }
          },
          VELOCITY_CONFIG_LIST_ID_DEFAULT
        )
      );
    } else if (!!configId && !isDefault) {
      setLoading(false);
    }
  }, [isDefault]);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("velocity_configs", "list", "-1"));
    };
  }, []);

  useEffect(() => {
    if (isDefault) {
      const { loading, error } = configListState;
      if (!loading && loading !== undefined) {
        if (error !== undefined && error !== false) {
          notification.error({ message: "Failed to Fetch Data. Please try again" });
          history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES._ROOT}`);
        }
        const { data } = configListState;
        const velocityConfigsList = data.records || [];
        const defaultConfig = velocityConfigsList[0];
        if (defaultConfig?.id) {
          configId = defaultConfig.id;
          history.replace(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}?configId=${configId}`);
        } else {
          // Default config is required so will always exist but adding this just for sake of error handling
          notification.error({ message: "Failed to find default config" });
          history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES._ROOT}`);
        }
      }
    }
  }, [configListState]);

  if (loading || isDefault) {
    return <Loader />;
  }

  return <>{isNew ? <VelocityConfigCreate /> : <VelocityConfigEdit configId={configId} stageName={stageName} />}</>;
};

export default VelocityConfigsCreateEditPage;
