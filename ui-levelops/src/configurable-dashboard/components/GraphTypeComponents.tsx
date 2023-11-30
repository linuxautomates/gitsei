import React, { useMemo, useCallback } from "react";
import { Form, Radio } from "antd";

import { WidgetType } from "../../dashboard/helpers/helper";

interface GraphTypeComponentProps {
  graphType: string;
  widgetType: string;
  onWidgetTypeSelection: (value: string) => void;
}

const GraphTypeComponent: React.FC<GraphTypeComponentProps> = ({ graphType, widgetType, onWidgetTypeSelection }) => {
  const getGraphTypeOptions = useMemo(() => {
    if (widgetType?.includes(WidgetType.STATS)) {
      return [
        { label: "Stat", value: WidgetType.STATS },
        { label: "Stat From Tables", value: WidgetType.CONFIGURE_WIDGET_STATS }
      ];
    }
    return [
      { label: "Single Report", value: WidgetType.GRAPH },
      { label: "Multi Report (Time Series)", value: WidgetType.COMPOSITE_GRAPH },
      { label: "Report From Tables", value: WidgetType.CONFIGURE_WIDGET }
    ];
  }, []);

  const handleWidgetTypeSelection: any = useCallback((e: any) => onWidgetTypeSelection(e.target.value), []);

  return (
    <Form.Item
      className="edit-widget"
      required
      label="Widget Type"
    >
      <Radio.Group
        value={graphType}
        onChange={handleWidgetTypeSelection}>
        {getGraphTypeOptions.map((option: any) => (
          <Radio key={option.value} value={option.value}>
            {option.label}
          </Radio>
        ))}
      </Radio.Group>
    </Form.Item>
  );
};

export default React.memo(GraphTypeComponent);
