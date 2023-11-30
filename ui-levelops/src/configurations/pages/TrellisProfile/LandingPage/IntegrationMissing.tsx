import { Divider } from "antd";
import { getIntegrationPage } from "constants/routePaths";
import React from "react";
import { useHistory } from "react-router-dom";
import { AntButton, AntText } from "shared-resources/components";
import "./IntegrationMissing.scss";

const IntegrationMissing = () => {
  const history = useHistory();

  return (
    <div className="integration-missing-page">
      <Divider className="divider-spacing" />
      <AntText className="content">
        The Trellis Score is computed by assessing the team or member-level data from multiple integrations including
        issue management applications such as Jira, SCM applications such as GitLab and GitHub. Integrate with an SCM or
        issue management application to get started.
      </AntText>
      <AntButton type="primary" className="add-integration-button" onClick={() => history.push(getIntegrationPage())}>
        Add Integration
      </AntButton>
    </div>
  );
};

export default IntegrationMissing;
