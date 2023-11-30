import React from "react";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
const marginStyle = {
  margin: "5px"
};

export function closeButton(props) {
  return (
    <button aria-hidden="true" className="close" name={props.name} onClick={props.onClick} id={props.id}>
      ×
    </button>
  );
}
export function checkmarkButton(props) {
  return (
    <AntButton checkmark round fill onClick={props.onClick} id={props.id}>
      <i className="fa fa-check" />
    </AntButton>
  );
}

function _button(props, image) {
  return (
    <button type="button" aria-hidden="true" className="close" style={marginStyle} id={props.id}>
      <i className={image} onClick={props.onClick} />
    </button>
  );
}
