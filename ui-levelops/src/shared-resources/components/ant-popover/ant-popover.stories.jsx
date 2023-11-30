import React from "react";
import { AntPopover, AntButton } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

const content = (
  <div>
    <p>Content</p>
    <p>Content</p>
  </div>
);

export default {
  title: "Ant Popover"
};

export const Popover = () => (
  <AntPopover content={content} title="Title">
    Some text here !!!
  </AntPopover>
);

export const PopoverWithTrigger = () => (
  <>
    <AntPopover content={content} title="Title" trigger="hover">
      Hover me
    </AntPopover>
    <br />
    <br />
    <AntPopover content={content} title="Title" trigger="focus">
      <AntButton type="primary">Focus me</AntButton>
    </AntPopover>
    <br />
    <br />
    <AntPopover content={content} title="Title" trigger="click">
      Click me
    </AntPopover>
  </>
);

export const PopoverPlacement = () => (
  <AntPopover placement="right" content={content} title="Title">
    Some text here !!!
  </AntPopover>
);
