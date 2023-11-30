import React from "react";
// @ts-ignore
import styled from "styled-components";
import { ICanvasOuterDefaultProps, ICanvasInnerDefaultProps } from "@mrblenny/react-flow-chart";

export const CanvasOuterCustom = React.forwardRef(
  ({ children, ...otherProps }: ICanvasOuterDefaultProps, ref: React.Ref<HTMLDivElement>) => {
    return (
      <div
        ref={ref}
        style={{
          position: "relative",
          backgroundSize: "20px 20px",
          backgroundColor: "rgba(0,0,0,0.08)",
          backgroundImage:
            "linear-gradient(90deg,hsla(0,0%,100%,.2) 1px,transparent 0),\n" +
            "    linear-gradient(180deg,hsla(0,0%,100%,.2) 1px,transparent 0",

          //width: "10000px",
          //height: "10000px",
          overflow: "hidden",
          //cursor: "not-allowed",
          display: "flex"
        }}
        {...otherProps}>
        {children}
      </div>
    );
  }
);

export const CanvasInnerCustom = styled.div<ICanvasInnerDefaultProps>`
  position: relative;
  outline: 1px dashed rgba(0, 0, 0, 0.1);
  width: 10000px;
  height: 10000px;
  overflow: hidden;
  cursor: all-scroll;
` as any;
