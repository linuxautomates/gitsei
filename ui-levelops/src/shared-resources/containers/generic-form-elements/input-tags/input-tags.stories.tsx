import React from "react";
import InputTagsComponent from "./input-tags.component";

export default {
  title: "Input tags",
  component: InputTagsComponent
};

export const InputTags = () => {
  let tags = ["Apple", "Mango", "Banana", "Orange"];
  return (
    <InputTagsComponent
      value={tags}
      onChange={(newTags: string[]) => {
        tags = newTags;
      }}/>
  )
};
