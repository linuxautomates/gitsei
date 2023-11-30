import React from "react";
import { AntRow, AntCol } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Row",
  component: AntRow
};

export const Row = () => (
  <AntRow gutter={[10, 10]}>
    <AntCol span={6}>1</AntCol>
    <AntCol span={6}>2</AntCol>
    <AntCol span={6}>3</AntCol>
    <AntCol span={6}>4</AntCol>
  </AntRow>
);
