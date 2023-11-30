import React from "react";
import { AntCol } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Col",
  component: AntCol
};

export const Select = () => (
  <>
    <AntCol span={6}>1</AntCol>
    <AntCol span={6}>2</AntCol>
    <AntCol span={6}>3</AntCol>
    <AntCol span={6}>4</AntCol>
  </>
);
