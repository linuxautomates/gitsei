import * as React from "react";
import { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { getIngestionIntegrationStatus } from "reduxConfigs/actions/restapi/ingestion.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";
import { Tooltip } from "antd";
import {
  IngestionIntegrationStatus,
  IngestionStatus
} from "reduxConfigs/actions/restapi/response-types/ingestionResponseType";
import Loader from "components/Loader/Loader";
import { SuccessStatus, FailedStatus, WarningStatus } from "shared-resources/components";
import IntegrationStatusMessage, { STATUS_NOT_AVAILABLE } from "./constants";

interface IntegrationStatusProps {
  id: string;
  application: string;
}

const IntegrationStatusComponent: React.FC<IntegrationStatusProps> = ({ id, application }) => {
  const dispatch = useDispatch();

  const [loadingStatus, setLoadingStatus] = useState<boolean>(true);
  const [ingestionStatusData, setIngestionStatusData] = useState<IngestionIntegrationStatus>();

  const integrationStatusState = useParamSelector(getGenericRestAPISelector, {
    uri: "ingestion_integration_status",
    method: "get",
    uuid: id
  });

  useEffect(() => {
    const data = get(integrationStatusState, "data", {});
    if (Object.keys(data).length === 0) {
      dispatch(getIngestionIntegrationStatus(id));
    }
  }, []);

  useEffect(() => {
    if (loadingStatus) {
      const loading = get(integrationStatusState, "loading", true);
      const error = get(integrationStatusState, "error", true);
      if (!loading && !error) {
        const data = get(integrationStatusState, ["data"], {});
        setIngestionStatusData(data);
        setLoadingStatus(false);
      }
    }
  }, [integrationStatusState]);

  const textStyle = useMemo(
    () => ({
      fontSize: "16px",
      lineHeight: "28px"
    }),
    []
  );

  const getStatus = useMemo(() => {
    if (ingestionStatusData) {
      switch (ingestionStatusData.status) {
        case IngestionStatus.HEALTHY:
          return <SuccessStatus textStyle={textStyle} text={ingestionStatusData.status} />;
        case IngestionStatus.WARNING:
        case IngestionStatus.UNKNOWN:
          return <WarningStatus textStyle={textStyle} text={ingestionStatusData.status} />;
        case IngestionStatus.FAILED:
          return <FailedStatus textStyle={textStyle} />;
        default:
          return null;
      }
    }
    return null;
  }, [ingestionStatusData]);

  if (loadingStatus) {
    return <Loader />;
  }

  const getTooltipMessage = () => {
    if (ingestionStatusData) {
      if (ingestionStatusData.status in IntegrationStatusMessage) {
        let messagesByStatus: any = IntegrationStatusMessage[ingestionStatusData.status];

        // Check if specific message for application is set
        if (application in messagesByStatus) {
          return messagesByStatus[application];
        } else {
          // Otherwise, use the global status message
          return messagesByStatus["*"];
        }
      }
      return STATUS_NOT_AVAILABLE;
    }
    return STATUS_NOT_AVAILABLE;
  };

  return (
    <Tooltip title={getTooltipMessage()}>
      <div className="ingestion-integration-status">{getStatus}</div>
    </Tooltip>
  );
};

export default React.memo(IntegrationStatusComponent);
