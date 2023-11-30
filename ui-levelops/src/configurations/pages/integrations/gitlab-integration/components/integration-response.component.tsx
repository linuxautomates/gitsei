import * as React from "react";
import { Col, Result, Row } from "antd";
import { useParams } from "react-router-dom";
import {
  ERROR,
  FAILED_ADD_INTEGRATION_WARNING,
  PRE_FLIGHT_WARNING,
  SUCCESS,
  SUCCESS_ADD_INTEGRATION
} from "../../../../../constants/formWarnings";
import { getHomePage, getIntegrationPage } from "../../../../../constants/routePaths";
import { AntButton } from "../../../../../shared-resources/components";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { PreFlightCheck } from "configurations/components/integrations";
import { useDispatch } from "react-redux";
import { ProjectPathProps } from "@harness/microfrontends/dist/modules/10-common/interfaces/RouteInterfaces";

interface IntegrationResponseProps {
  error: boolean;
  createLoading: boolean;
  apiKeyLoading: boolean;
  history: any;
  integrationType: any;
  integrationFormName: string;
  integrationCreateState: any;
  setCreated: any;
  setError: any;
  setParamsState: any;
  createIntegration: (integrations?: any, disablePreflightCheck?: boolean) => void;
}

export const IntegrationResponse: React.FC<IntegrationResponseProps> = ({
  createLoading,
  error,
  apiKeyLoading,
  history,
  integrationType,
  integrationFormName,
  integrationCreateState,
  setCreated,
  setError,
  setParamsState,
  createIntegration
}) => {
  const dispatch = useDispatch();
  const projectParams = useParams<ProjectPathProps>();

  const status = !createLoading && !error ? SUCCESS : ERROR;
  if (status === SUCCESS) {
    if (!apiKeyLoading) {
      history.push(`${getHomePage(projectParams)}`);
    }
  }
  const title = !createLoading && !error ? SUCCESS_ADD_INTEGRATION : FAILED_ADD_INTEGRATION_WARNING;
  const subTitle =
    !createLoading && !error
      ? `${(integrationType || "").toUpperCase()} integration ${integrationFormName}`
      : PRE_FLIGHT_WARNING;
  const checks =
    (integrationCreateState["0"].data.preflight_check && integrationCreateState["0"].data.preflight_check.checks) || [];
  const extras =
    !createLoading && !error ? (
      <AntButton type={"primary"} onClick={() => history.push(getIntegrationPage())}>
        Return to Integrations
      </AntButton>
    ) : (
      <Row type={"flex"} justify={"center"} gutter={[10, 10]}>
        <Col span={12}>
          <PreFlightCheck checks={checks} />
        </Col>
        <Col span={24}>
          <AntButton
            type={"primary"}
            onClick={(e: any) => {
              dispatch(restapiClear("integrations", "create", "0"));
              setCreated(false);
              setError(false);
              setParamsState(undefined);
            }}>
            Fix Credentials
          </AntButton>
        </Col>
        <Col span={24}>
          <AntButton
            type={"primary"}
            onClick={(e: any) => {
              dispatch(restapiClear("integrations", "create", "0"));
              setCreated(false);
              setError(false);
              createIntegration(null, true);
            }}>
            Skip Preflight Check (Warning: the scan may not work correctly)
          </AntButton>
        </Col>
      </Row>
    );

  return <Result status={status} title={title} subTitle={subTitle} extra={extras} />;
};
