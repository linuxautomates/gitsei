import React from "react";
import * as PropTypes from "prop-types";
import { Descriptions } from "antd";
import { SvgIcon } from "shared-resources/components";
import { getIntegrationUrlMap } from "../../../../constants/integrations";

export class PreviewContainer extends React.PureComponent {
  constructor(props) {
    super(props);
  }

  render() {
    const { integration } = this.props;
    const integrationType = integration.type || "";
    const integrationForm = integration.form || {};
    let formFields = [];
    if (integration.type && integration.form) {
      const integrationMap = getIntegrationUrlMap();
      formFields = integrationMap[integrationType].form_fields;
    }
    return (
      <Descriptions
        title={
          <div>
            <SvgIcon icon={integrationType} style={{ height: "2.4rem", width: "2.4rem" }} />
            {integrationType}
          </div>
        }
        layout={"horizontal"}>
        {formFields.map(key => (
          <Descriptions.Item label={key} span={24}>
            {integrationForm[key]}
          </Descriptions.Item>
        ))}
      </Descriptions>
    );
  }
}

PreviewContainer.propTypes = {
  integration: PropTypes.object.isRequired
};

PreviewContainer.defaultProps = {
  integration: {
    type: "",
    form: {}
  }
};
