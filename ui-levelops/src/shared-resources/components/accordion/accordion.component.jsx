import React from "react";
import * as PropTypes from "prop-types";
import { Collapse } from "antd";

import "./accordion.local.scss";

export class AccordionComponent extends React.PureComponent {
  render() {
    const { className } = this.props;
    const { Panel } = Collapse;

    return (
      <div className={className}>
        <Collapse accordion>
          {this.props.panels.map(panel => (
            <Panel />
          ))}
        </Collapse>
      </div>
    );
  }
}

AccordionComponent.propTypes = {
  className: PropTypes.string
};

AccordionComponent.defaultProps = {
  className: "accordion"
};
