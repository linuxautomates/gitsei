import React from "react";
import { AntParagraph } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Paragraph",
  component: AntParagraph
};

export const Paragraph = () => (
  <>
    <AntParagraph>
      Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraphAnt Design AntParagraph Ant
      Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraph Ant Design AntParagraph
    </AntParagraph>
  </>
);

export const ParagraphWithEllipsis = () => (
  <>
    <AntParagraph ellipsis>
      Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraphAnt Design AntParagraph Ant
      Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraph Ant Design AntParagraph
    </AntParagraph>
  </>
);

export const ParagraphWithMultilineEllipsis = () => (
  <>
    <AntParagraph ellipsis={{ rows: 3 }}>
      Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraphAnt Design AntParagraph Ant
      Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraph Ant Design AntParagraphAnt Design AntParagraph Ant Design AntParagraph Ant
      Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraphAnt Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant
      Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph
    </AntParagraph>
  </>
);

export const ParagraphWithMultilineEllipsisAndExpandable = () => (
  <>
    <AntParagraph ellipsis={{ rows: 3, expandable: true }}>
      Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraphAnt Design AntParagraph Ant
      Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraph Ant Design AntParagraphAnt Design AntParagraph Ant Design AntParagraph Ant
      Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph Ant Design AntParagraphAnt Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant
      Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design AntParagraph Ant Design
      AntParagraph
    </AntParagraph>
  </>
);
