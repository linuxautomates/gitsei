import React from "react";
import { Drawer, Icon, Typography } from "antd";
import * as PropTypes from "prop-types";

const { Text } = Typography;

export class AntDrawerWrapperComponent extends React.Component {
  get title() {
    const { name, icon, name_editable } = this.props;
    return (
      <div className="flex align-center">
        <Icon className="mr-5" type={icon} />
        <Text editable={name_editable}>{name}</Text>
      </div>
    );
  }

  render() {
    const { onClose, visible, children, placement, width, className } = this.props;
    return (
      <Drawer
        title={this.title}
        width={width}
        placement={placement}
        onClose={onClose}
        visible={visible}
        getContainer={false}
        className={className}
        closable
        destroyOnClose>
        {children}
      </Drawer>
    );
  }
}

AntDrawerWrapperComponent.propTypes = {
  icon: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  visible: PropTypes.bool.isRequired,
  placement: PropTypes.string,
  width: PropTypes.string,
  name_editable: PropTypes.bool
};

AntDrawerWrapperComponent.defaultProps = {
  placement: "right",
  width: "356px",
  name_editable: true
};

export default AntDrawerWrapperComponent;
