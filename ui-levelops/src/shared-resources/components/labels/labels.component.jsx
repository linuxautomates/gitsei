import React from "react";
import "./labels.style.scss";

export const LabelComponent = props => {
  const { type, text } = props;
  let className = "labels";
  const style = { overflow: "hidden", textOverflow: "ellipsis" };
  switch (type) {
    case "title":
      className = `${className} ${className}__title`;
      break;
    case "description":
      className = `${className} ${className}__description`;
      break;
    case "info":
      className = `${className} ${className}__info`;
      break;
    case "item":
      className = `${className} ${className}__item`;
      break;
    case "item-label":
      className = `${className} ${className}__item-label`;
      break;
    case "header":
      className = `${className} ${className}__header`;
      break;
    case "link":
      className = `${className} ${className}__link`;
      break;
    case "badge":
      className = `${className} ${className}__badge`;
      break;
    default:
      className = `${className}`;
  }
  return (
    <span className={className} style={style}>
      {text}
    </span>
  );
};
