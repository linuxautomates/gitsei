import React, { useMemo } from "react";
import "./scmConfigurations.scss";
import { Collapse, Card } from "antd";
import Icon from "antd/lib/icon";
import ScmDefinition from "./ScmDefinition";
import { SCMFilter } from "classes/RestWorkflowProfile";
import { AntText, AntTooltip } from "shared-resources/components";
import { CICD_FOOTER_INFO, TOTAL_DEPLOYMENT_INFO, WORKFLOW_PROFILE_TABS } from "../constant";

interface SCMConfigurationsProps {
  config: SCMFilter;
  calculationType: string;
  onChange: (value: SCMFilter) => void;
  filterString: string;
}

const SCMConfigurations = (props: SCMConfigurationsProps) => {
  const { config, onChange, calculationType, filterString } = props;

  const renderFooterData = useMemo(() => {
    return (
      <div className="footer mt-15 mb-15">
        <Icon type="info-circle" className="footer-icon mr-5" /> {CICD_FOOTER_INFO}
      </div>
    );
  }, []);

  return (
    <div className="scm-configurations-container">
      <Card className="scm-card">
        <Collapse bordered={false} className="scm-collapse" defaultActiveKey={[calculationType]}>
          <Collapse.Panel
            key={calculationType}
            header={
              <div className="flex">
                <AntText className="mr-10" strong>{`Define ${filterString}`}</AntText>
                {calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB && (
                  <AntTooltip title={TOTAL_DEPLOYMENT_INFO}>
                    <Icon type="info-circle" theme="outlined" className="mt-5" />
                  </AntTooltip>
                )}
              </div>
            }>
            <ScmDefinition
              filterKey={filterString}
              config={config}
              onChange={onChange}
              calculationType={calculationType}
            />
          </Collapse.Panel>
        </Collapse>
      </Card>
      {(calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB ||
        calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB) &&
        renderFooterData}
    </div>
  );
};

export default SCMConfigurations;
