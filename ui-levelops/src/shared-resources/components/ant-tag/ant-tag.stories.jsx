import React from "react";
import { AntTag } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

function log() {
  console.log("closable tag");
}

function preventDefault(e) {
  e.preventDefault();
}

const colors = [
  "red",
  "yellow",
  "orange",
  "cyan",
  "green",
  "blue",
  "purple",
  "geekblue",
  "magenta",
  "volcano",
  "gold",
  "lime",
  "#ff0",
  "#ccc",
  "#90a"
];

export default {
  title: "Ant Tag"
};

export const Tag = () => (
  <>
    <AntTag>Tag 1</AntTag>
    <AntTag>
      <a target="_blank" href="https://github.com/ant-design/ant-design/issues/1862">
        Link
      </a>
    </AntTag>
    <AntTag closable onClose={log}>
      Closable Tag
    </AntTag>
    <AntTag closable onClose={preventDefault}>
      Prevent Default
    </AntTag>
  </>
);

export const ColourfulTags = () => (
  <>
    {colors.map(color => (
      <AntTag key={color} color={color}>
        {color}
      </AntTag>
    ))}
  </>
);
