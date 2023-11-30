import React from "react";
import { AntModal } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Modal"
};

export const Modal = () => (
  <AntModal title="Basic Modal" closable={false}>
    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna
    aliqua.
  </AntModal>
);
