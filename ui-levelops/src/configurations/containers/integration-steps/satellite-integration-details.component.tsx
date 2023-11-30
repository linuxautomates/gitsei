import { Col, Row } from "antd";
import { getIntegrationUrlMap } from "constants/integrations";
import { get } from "lodash";
import React, { useCallback, useMemo } from "react";
import { useSelector } from "react-redux";
import { AntButton, AntCard, AntText, AntTitle } from "shared-resources/components";
import { IntegrationDetails } from ".";
import { DOCS_ROOT, DOCS_PATHS } from "constants/docsPath";

import "./satellite-integration-details.component.scss";
export interface SatelliteIntegrationDetailsProps {
  createIntegration: () => {};
  valid: () => {};
}
const SatelliteIntegrationDetails: React.FC<SatelliteIntegrationDetailsProps> = (
  props: SatelliteIntegrationDetailsProps
) => {
  const integrationType = useSelector((state: any) => get(state.integrationReducer.toJS(), ["integration_type"]));
  const integration_Form = useSelector((state: any) => get(state.integrationReducer.toJS(), ["integration_form"], {}));

  const newDetailsData = useMemo(() => {
    const integrationMap = getIntegrationUrlMap();
    return get(integrationMap, [integrationType, "newDetailsData"], {});
  }, [integrationType]);

  const submithandler = useCallback(() => {
    props.createIntegration();
  }, [integration_Form]);

  return (
    <div className="flex direction-column new-integration-card">
      <AntCard className="p-5 mt-10 new-integration-card__ant-card">
        <Row gutter={[40, 20]} className="new-integration-card__ant-card__row">
          <Col span={integration_Form.satellite ? 12 : 24}>
            <IntegrationDetails />
          </Col>
          {integration_Form.satellite && (
            <Col span={12}>
              <div className="flex direction-column">
                <AntTitle level={4} className="new-integration-card__ant-card__row__antTitle">
                  {(newDetailsData as any).heading}
                </AntTitle>
                <AntText className="new-integration-card__ant-card__row__antText">
                  {(newDetailsData as any).description}
                </AntText>
                <AntButton icon="download" type="primary" onClick={submithandler} disabled={!props.valid()}>
                  {(newDetailsData as any).buttonText}
                </AntButton>
                <AntButton
                  className="new-integration-card__ant-card__row__antButton"
                  type="link"
                  href={DOCS_ROOT + DOCS_PATHS.PROPELO_INGESTION_SATELLITE}
                  target="_blank"
                  rel="noopener noreferrer">
                  How to install a satellite
                </AntButton>
              </div>
            </Col>
          )}
        </Row>
      </AntCard>
    </div>
  );
};

export default SatelliteIntegrationDetails;
