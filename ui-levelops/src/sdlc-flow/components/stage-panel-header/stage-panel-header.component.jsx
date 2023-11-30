import React from "react";
import { Avatar } from "antd";
import * as PropTypes from "prop-types";
import { getTagsStyle } from "utils/tagsUtils";
import { SvgIcon } from "shared-resources/components";
import "./stage-panel-header.style.scss";

export class StagePanelHeaderComponent extends React.PureComponent {
  render() {
    const { className, violations, integrations } = this.props;

    return (
      <div className={`${className} flex justify-end align-center`}>
        <div className={`${className}__section flex align-center`}>
          <div className={`${className}__label medium-12`}>
            {" "}
            Security
            <br />
            Violations
          </div>
          <Avatar shape="square" style={getTagsStyle(violations.security)}>
            {violations.security || 0}
          </Avatar>
        </div>
        <div className={`${className}__section flex align-center `}>
          <div className={`${className}__label medium-12`}>
            {" "}
            Engineering
            <br />
            Violations
          </div>
          <Avatar shape="square" style={getTagsStyle(violations.engineering)}>
            {violations.engineering || 0}
          </Avatar>
        </div>
        <div className={`${className}__section flex align-center justify-end`}>
          {integrations.map(integration => (
            <SvgIcon style={{ width: "1.8rem", height: "1.8rem" }} icon={integration} />
          ))}
        </div>
      </div>
    );
  }
}

StagePanelHeaderComponent.propTypes = {
  className: PropTypes.string,
  integrations: PropTypes.arrayOf(PropTypes.string),
  violations: PropTypes.shape({
    security: PropTypes.number,
    engineering: PropTypes.number
  })
};

StagePanelHeaderComponent.defaultProps = {
  className: "stage-panel-header",
  integrations: [],
  violations: {
    security: 0,
    engineering: 0
  }
};
