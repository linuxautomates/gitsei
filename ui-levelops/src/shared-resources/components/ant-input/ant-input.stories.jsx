import React from "react";
import { AntInput, AntSelect } from "shared-resources/components";
import { Select } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

const selectBefore = (
  <AntSelect defaultValue="Http://">
    <Select.Option value="Http://">Http://</Select.Option>
    <Select.Option value="Https://">Https://</Select.Option>
  </AntSelect>
);
const selectAfter = (
  <AntSelect defaultValue=".com">
    <Select.Option value=".com">.com</Select.Option>
    <Select.Option value=".jp">.jp</Select.Option>
    <Select.Option value=".cn">.cn</Select.Option>
    <Select.Option value=".org">.org</Select.Option>
  </AntSelect>
);

export default {
  title: "Ant Input"
  // component: AntButton,
};

export const Input = () => <AntInput placeholder="Default Input" />;

export const InputSizes = () => (
  <>
    <AntInput size="small" placeholder="Small Input" style={{ marginBottom: "10px" }} />
    <AntInput placeholder="Default Input" style={{ marginBottom: "10px" }} />
    <AntInput size="large" placeholder="Large Input" style={{ marginBottom: "10px" }} />
  </>
);

export const DisabledInput = () => <AntInput disabled placeholder="Disabled Input" />;

export const Searchbox = () => <AntInput type="search" placeholder="Search Box" />;

export const SearchboxWithButton = () => <AntInput type="search" enterButton="Search" placeholder="Search Box" />;

export const Number = () => <AntInput type="number" min={1} max={10} defaultValue={3} />;

export const Textarea = () => <AntInput type="textarea" placeholder="Textarea" />;

export const Password = () => <AntInput type="password" placeholder="Password" />;

export const Email = () => <AntInput type="email" placeholder="Email" />;

export const InputGroup = () => (
  <AntInput type="group" compact>
    <AntInput type="search" placeholder="Search Input" style={{ width: "30%" }} />
    <AntInput placeholder="Default Input" style={{ width: "30%" }} />
  </AntInput>
);

export const InputGroupWithSelect = () => (
  <AntInput type="group" compact>
    <AntSelect defaultValue="2" style={{ width: "30%" }}>
      <Select.Option value="1">1</Select.Option>
      <Select.Option value="2">2</Select.Option>
      <Select.Option value="3">3</Select.Option>
    </AntSelect>
    <AntInput placeholder="Default Input" style={{ width: "30%" }} />
  </AntInput>
);

export const InputWithAddons = () => (
  <AntInput addonBefore={selectBefore} addonAfter={selectAfter} defaultValue="mysite" />
);
