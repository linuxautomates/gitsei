import React from "react";
import * as PropTypes from "prop-types";
import { Card } from "antd";
import { omit } from "lodash";

import "./ant-card.style.scss";

// in order to avoid conflicts with the existing Card Component, until we can use this as AntCard,
// import into the file it needs to be used as: import { AntCard } from 'shared-resources/components';
export class AntCardComponent extends React.PureComponent {
  render() {
    const props = this.props;
    return (
      <div className="ant-card-wrapper" onClick={this.props.onClickEvent} role="presentation">
        <Card {...omit(props, "onClickEvent")} bordered={true} style={{ ...props.style }}>
          {props.children}
        </Card>
      </div>
    );
  }
}

// we only need to pass the props that are necessary for our card. Default values will be provided for the rest of them
AntCardComponent.propTypes = {
  className: PropTypes.string,
  title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  bordered: PropTypes.bool,
  loading: PropTypes.bool,
  style: PropTypes.object, // so we can apply some specific style only for the desired card
  extra: PropTypes.any, // displayed on top right corner, near title. we can use this to toggle a menu with card actions
  size: PropTypes.oneOf(["small", "default"]),
  actions: PropTypes.object, // usually an array with the components displayed on card bottom
  children: PropTypes.node, // to load card content,
  tabList: PropTypes.array, // Array<{key: string, tab: ReactNode}>
  activeTabKey: PropTypes.string, //the key of the default selected tab
  onTabChange: PropTypes.func, // handle for the tab change
  onClickEvent: PropTypes.func // handle on card click
};

AntCardComponent.defaultProps = {
  title: "",
  bordered: false,
  style: {},
  extra: null,
  size: "default",
  actions: {},
  children: "",
  loading: false,
  tabList: null,
  activeTabKey: "",
  onTabChange: () => null,
  onClickEvent: () => null
};
