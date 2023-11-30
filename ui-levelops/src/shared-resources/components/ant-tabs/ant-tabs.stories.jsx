import React from "react";
import { AntTabs } from "shared-resources/components";
import { Icon } from "antd";
import { tabsData } from "./ant-tabs.helper";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Tabs",
  components: AntTabs
};

const tabpanes = [
  {
    tab: "Tab 1",
    key: "1",
    content: "Content of Tab Pane 1"
  },
  {
    tab: "Tab 2",
    key: "2",
    content: "Content of Tab Pane 2"
  },
  {
    tab: "Tab 3",
    key: "3",
    content: "Content of Tab Pane 3"
  }
];

const tabpanesWithDisabled = [
  {
    tab: "Tab 1",
    key: "1",
    content: "Content of Tab Pane 1"
  },
  {
    tab: "Tab 2",
    key: "2",
    content: "Content of Tab Pane 2",
    disabled: true
  },
  {
    tab: "Tab 3",
    key: "3",
    content: "Content of Tab Pane 3"
  }
];

const tabWithIcon = [
  {
    tab: (
      <span>
        <Icon type="apple" />
        Tab 1
      </span>
    ),
    key: "1",
    content: "Content of Tab Pane 1"
  },
  {
    tab: (
      <span>
        <Icon type="android" />
        Tab 2
      </span>
    ),
    key: "2",
    content: "Content of Tab Pane 2"
  }
];

export const Tabs = () => <AntTabs defaultActiveKey={0} tabpanes={tabpanes} />;

export const TabDisabled = () => <AntTabs defaultActiveKey={0} tabpanes={tabpanesWithDisabled} />;

export const TabWithIcon = () => <AntTabs defaultActiveKey={0} tabpanes={tabWithIcon} />;

export const TabWithSlide = () => <AntTabs defaultActiveKey={0} tabpanes={tabsData} />;
