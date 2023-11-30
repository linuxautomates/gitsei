import { Typography } from "antd";
import { RestIntegrations } from "classes/RestIntegrations";
import React from "react";
import { useDispatch } from "react-redux";
import { satelliteIntegrationYAMLDownload } from "reduxConfigs/actions/restapi";
import { AntButton } from "shared-resources/components";
import { SelfOnboardingFormFields } from "../../constants";
import "./satelliteOptions.styles.scss";
import { useAppStore } from "contexts/AppStoreContext";

interface SatelliteOptionFinalProps {
  getFromSelfOnboardingForm: (key: string) => any;
  integrationPayload: RestIntegrations;
}
const SatelliteOptionFinalComponent: React.FC<SatelliteOptionFinalProps> = ({
  integrationPayload,
  getFromSelfOnboardingForm
}) => {
  const { accountInfo } = useAppStore();
  const dispatch = useDispatch();
  const handleYAMLDownload = () => {
    const tenant = window.isStandaloneApp
      ? localStorage.getItem("levelops_user_org") || ""
      : (accountInfo?.identifier || "").toLowerCase();
    dispatch(
      satelliteIntegrationYAMLDownload(
        integrationPayload,
        getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID),
        tenant
      )
    );
  };
  return (
    <div className="satellite-final-component">
      <Typography.Paragraph>Successfully Submitted!</Typography.Paragraph>
      <Typography.Paragraph>
        Download the satellite configuration file for this <br />
        integration and follow the provided link to update
        <br /> your SEI Satellite
      </Typography.Paragraph>
      <AntButton onClick={handleYAMLDownload}>Download Config</AntButton>
    </div>
  );
};

export default SatelliteOptionFinalComponent;
