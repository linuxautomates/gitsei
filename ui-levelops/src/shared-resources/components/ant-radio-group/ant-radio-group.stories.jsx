import React from "react";
import { AntRadioGroup } from "shared-resources/components";
import { Radio } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Radio Group",
  components: AntRadioGroup
};

export const RadioGroup = () => (
  <AntRadioGroup name="radiogroup" defaultValue={3}>
    <Radio value={1}>A</Radio>
    <Radio value={2}>B</Radio>
    <Radio value={3}>C</Radio>
    <Radio value={4}>2</Radio>
  </AntRadioGroup>
);

export const RadioButtonGroup = () => (
  <Radio.Group defaultValue="a">
    <Radio.Button value="a">Hangzhou</Radio.Button>
    <Radio.Button value="b">Shanghai</Radio.Button>
    <Radio.Button value="c">Beijing</Radio.Button>
    <Radio.Button value="d">Chengdu</Radio.Button>
  </Radio.Group>
);
