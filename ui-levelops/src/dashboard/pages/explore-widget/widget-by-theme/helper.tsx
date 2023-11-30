import * as React from "react";
import { b64DecodeUnicode } from "../../../../utils/stringUtils";

export const getParagraphFromOverview = (id: string, description?: string) => {
  if (!description) {
    return "";
  }

  return description.length > 130 ? description.slice(0, 130) + "..." : description;
};

export const getParagraphFromOverviewDora = (id: string, description?: string) => {
  if (!description) {
    return "";
  }
  return description;
};
