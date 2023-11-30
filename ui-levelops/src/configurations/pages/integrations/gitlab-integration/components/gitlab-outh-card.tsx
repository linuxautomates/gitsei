import React, { useState } from "react";
import { Radio } from "antd";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { getIntegrationPage } from "constants/routePaths";
import { integrationSubType, integrationType } from "reduxConfigs/actions/integrationActions";
import { AntButton, AntCard, AntText } from "shared-resources/components";
import { IntegrationAuthTypes } from "../helpers";

const GitlabOuthCard: React.FC = () => {
  const [apiChoice, setApiChoice] = useState<undefined | IntegrationAuthTypes>(IntegrationAuthTypes.OAUTH);

  const dispatch = useDispatch();
  const history = useHistory();

  const basicIntegrationOptions = [
    {
      label: "OAuth",
      info: "",
      type: IntegrationAuthTypes.OAUTH
    },
    {
      label: "API Key",
      info: "",
      type: IntegrationAuthTypes.API_KEY
    }
  ];

  const onRadioSelect = (e: any) => {
    setApiChoice(e.target.value);
  };

  const handleSelectClick = () => {
    dispatch(integrationType("gitlab"));
    dispatch(integrationSubType(apiChoice));
    history.push(`${getIntegrationPage()}/new-add-integration-page`);
  };

  return (
    <AntCard
      title="GitLab.com"
      className="integration-type-card"
      actions={[
        <AntButton className="select-type-btn" type="secondary" onClick={handleSelectClick} disabled={!apiChoice}>
          Select
        </AntButton>
      ]}>
      <div className="card-content">
        <Radio.Group onChange={onRadioSelect} value={apiChoice}>
          {basicIntegrationOptions.map((option: any) => (
            <div className="flex direction-column">
              <Radio className="basic-integration-option" value={option.type}>
                {option.label}
              </Radio>
              <AntText className="basic-integration-label">{option.info}</AntText>
            </div>
          ))}
        </Radio.Group>
      </div>
    </AntCard>
  );
};

export default React.memo(GitlabOuthCard);
