import React from "react";
import { AntText } from "shared-resources/components";

export const formWarming = (warning: string) => {
  if (!warning) {
    return null;
  }
  const style = {
    color: " #EC5B56",
    fontSize: "14px"
  };
  return (
    <AntText style={style} id="form-warning">
      {warning}
    </AntText>
  );
};
