import React from "react";
import { AntText } from "../../../shared-resources/components";
import { Form, Slider } from "antd";
import { v1 as uuid } from "uuid";
import "./HygieneJiraFilter.scss";
import { hygieneTypes, zendeskSalesForceHygieneTypes } from "../../constants/hygiene.constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface HygieneJiraFilterProps {
  onFilterValueChange: (value: any, type?: any) => void;
  onWeightChange: (value: any, type?: any) => void;
  weightError: string;
  widgetWeights: any;
  application: string;
  reportType: string;
  filters: any;
  customHygienes?: Array<any>;
}

const HygieneJiraFilterComponent: React.FC<HygieneJiraFilterProps> = (props: HygieneJiraFilterProps) => {
  const { weightError, widgetWeights, application, onWeightChange } = props;
  const customHygienes = props.customHygienes ? props.customHygienes.map((hygiene: any) => hygiene.id) : [];

  const getHygieneTypes = () => {
    const types =
      application === IntegrationTypes.JIRA || application === IntegrationTypes.AZURE
        ? hygieneTypes
        : zendeskSalesForceHygieneTypes;
    return [
      ...types.map(t => ({ id: t, name: t })),
      ...(props.customHygienes || [])
      //...customHygienes.map(hy => hy.replace(/ /g, "_"))
    ];
  };

  const onChange = (value: any, type: any) => {
    const name = type.replace(/_/g, " ");
    if (customHygienes.includes(name)) {
      //const cHyiene = props.customHygienes && props.customHygienes.find((hy: any) => hy.name === name);
      onWeightChange(value, { id: type, type });
    } else {
      onWeightChange(value, type);
    }
  };

  return (
    <>
      <div>
        {getHygieneTypes().map((hygiene: any) => (
          <Form.Item key={hygiene.id} label={hygiene.name.replace(/_/g, " ")} colon={false}>
            <Slider
              value={widgetWeights[hygiene.id]}
              onChange={(value: any) => onChange(value, hygiene.id)}
              min={0}
              max={100}
            />
          </Form.Item>
        ))}
      </div>
      <AntText style={{ fontSize: "12px" }} type={"danger"}>
        {weightError}
      </AntText>
    </>
  );
};

export default HygieneJiraFilterComponent;
