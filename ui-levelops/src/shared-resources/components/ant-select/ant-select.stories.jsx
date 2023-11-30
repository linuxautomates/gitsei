import React from "react";
import { AntSelect } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

const selectOptions = ["Violet", "Green", "Blue", "Red"];

export default {
  title: "Ant Select",
  component: AntSelect
};

export const Select = () => <AntSelect defaultValue="Green" options={selectOptions} />;

export const DisabledSelect = () => <AntSelect defaultValue="Red" disabled options={selectOptions} />;
