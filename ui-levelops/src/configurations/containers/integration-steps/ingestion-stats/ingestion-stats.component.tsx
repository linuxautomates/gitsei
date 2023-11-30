import React, { useEffect, useMemo, useState } from "react";
import { Row, Col } from "antd";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";

import "./ingestion-stats.style.scss";
import { get } from "lodash";
import Loader from "components/Loader/Loader";
import { useDispatch } from "react-redux";
import { integrationsGet } from "reduxConfigs/actions/restapi";
import { IngestionStatPayload, ingestionStatsMapping } from "./ingestion-stats";
import { IngestionStat } from "..";

interface IntegrationStatsProps {
  id: string;
}

const IngestionStatsComponent: React.FC<IntegrationStatsProps> = ({ id }) => {
  const dispatch = useDispatch();

  const [integrationLoading, setIntegrationLoading] = useState<boolean>(true);
  const [application, setApplication] = useState<string>("");

  const integrationState = useParamSelector(getGenericRestAPISelector, {
    uri: "integrations",
    method: "get",
    uuid: id
  });

  const stats: IngestionStatPayload[] = useMemo(() => {
    if (!application) {
      return [];
    }

    return ingestionStatsMapping(id)[application] || [];
  }, [application, id]);

  useEffect(() => {
    if (integrationLoading) {
      const loading = get(integrationState, "loading", true);
      const error = get(integrationState, "error", true);
      const data = get(integrationState, "data", {});

      if (!loading && !error && Object.keys(data).length > 0) {
        setIntegrationLoading(false);
        const _application = get(data, "application", "");
        setApplication(_application);
      }

      if (!loading && (!data || Object.keys(data).length === 0)) {
        // do the API call
        dispatch(integrationsGet(id));
      }
    }
  }, [integrationState]);

  if (integrationLoading) {
    return <Loader />;
  }

  if (stats.length === 0) {
    return null;
  }

  return (
    <div className="ingestion-stats">
      <div className="ingestion-stats--title">Ingestion Stats</div>
      <div className="ingestion-stats__stats">
        <Row gutter={20}>
          {stats.map((stat: any) => (
            <Col span={4}>
              <IngestionStat key={stat.request.title} id={id} {...stat} />
            </Col>
          ))}
        </Row>
      </div>
    </div>
  );
};

export default React.memo(IngestionStatsComponent);
