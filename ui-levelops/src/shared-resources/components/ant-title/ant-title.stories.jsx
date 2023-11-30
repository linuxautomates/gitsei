import React from "react";
import { AntTitle } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Title",
  component: AntTitle
};

export const Title = () => (
  <>
    <AntTitle>h1. Ant Design</AntTitle>
    <AntTitle level={2}>h2. Ant Design</AntTitle>
    <AntTitle level={3}>h3. Ant Design</AntTitle>
    <AntTitle level={4}>h4. Ant Design</AntTitle>
  </>
);
