import React from "react";
import { Tabs } from "antd";

const { TabPane } = Tabs;

export const AntTabsComponent = props => {
  const makeTab = () => {
    const data = props.tabpanes;
    return data.map((tabPane, i) => {
      return (
        <TabPane key={tabPane.id || i} {...tabPane}>
          {tabPane.content}
        </TabPane>
      );
    });
  };

  return <Tabs {...props}>{makeTab()}</Tabs>;
};
