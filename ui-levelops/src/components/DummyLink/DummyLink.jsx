import React from "react";

const DummyLink = ({ onClick, children, ...props }) => (
  <a
    // eslint-disable-next-line jsx-a11y/anchor-is-valid
    href="#"
    onClick={evt => {
      evt.preventDefault();
      onClick && onClick();
    }}
    {...props}
  />
);

export default DummyLink;
