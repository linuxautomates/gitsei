import React from "react";
import { AntSwitch } from "shared-resources/components";
import { Icon } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Switch"
};

export const Switch = () => <AntSwitch />;
export const SwitchTypes = () => (
  <>
    <AntSwitch style={{ marginRight: "10px" }} checkedChildren="ON" unCheckedChildren="OFF" defaultChecked />
    <AntSwitch
      style={{ marginRight: "10px" }}
      checkedChildren={<Icon type="check" />}
      unCheckedChildren={<Icon type="close" />}
      defaultChecked
    />
  </>
);

export const SizedSwitch = () => (
  <>
    <AntSwitch style={{ marginRight: "10px" }} size="small" defaultChecked />
    <AntSwitch style={{ marginRight: "10px" }} />
  </>
);

export const DisabledSwitch = () => (
  <>
    <AntSwitch style={{ marginRight: "10px" }} disabled defaultChecked />
    <AntSwitch style={{ marginRight: "10px" }} disabled />
  </>
);
