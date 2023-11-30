import React from "react";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { Icon } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Button",
  component: AntButton
};

export const Button = () => <AntButton>Button</AntButton>;

export const ButtonStates = () => (
  <>
    <AntButton style={{ marginRight: "10px" }}>Button</AntButton>
    <AntButton style={{ marginRight: "10px" }} type="primary">
      Button
    </AntButton>
    <AntButton style={{ marginRight: "10px" }} type="dashed">
      Button
    </AntButton>
    <AntButton style={{ marginRight: "10px" }} type="danger">
      Button
    </AntButton>
  </>
);

export const BlockButton = () => (
  <>
    <AntButton block style={{ marginBottom: "10px" }}>
      Button
    </AntButton>
    <AntButton block style={{ marginBottom: "10px" }} type="primary">
      Button
    </AntButton>
    <AntButton block style={{ marginBottom: "10px" }} type="dashed">
      Button
    </AntButton>
    <AntButton block style={{ marginBottom: "10px" }} type="danger">
      Button
    </AntButton>
  </>
);

export const ButtonSize = () => (
  <>
    <AntButton style={{ marginRight: "10px" }} size="small">
      Small Button
    </AntButton>
    <AntButton style={{ marginRight: "10px" }}>Default Button</AntButton>
    <AntButton style={{ marginRight: "10px" }} size="large">
      Button
    </AntButton>
  </>
);

export const ButtonWithIcon = () => (
  <>
    <AntButton style={{ marginRight: "10px" }} icon="download" size="small">
      Small Button
    </AntButton>
    <AntButton style={{ marginRight: "10px" }} icon="download" type="primary">
      Default Button
    </AntButton>
    <AntButton style={{ marginRight: "10px" }} size="large">
      <Icon type="download" />
      Button
    </AntButton>
  </>
);

export const ButtonWithShapes = () => (
  <>
    <AntButton style={{ marginRight: "10px" }} shape="circle" icon="download" type="primary" />
    <AntButton style={{ marginRight: "10px" }} shape="round" icon="download" type="primary" />
    <AntButton style={{ marginRight: "10px" }} type="primary" shape="round" icon="download">
      Download
    </AntButton>
  </>
);

export const DisabledButton = () => <AntButton disabled>Disabled Button</AntButton>;
