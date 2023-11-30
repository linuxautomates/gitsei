import React from "react";
import { AntDrawer } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Drawer",
  components: AntDrawer
};

export const Drawer = () => (
  <AntDrawer title="Basic Drawer" placement="right" closable={true} onClose={false} visible={true}>
    <p>Some contents...</p>
    <p>Some contents...</p>
    <p>Some contents...</p>
  </AntDrawer>
);
