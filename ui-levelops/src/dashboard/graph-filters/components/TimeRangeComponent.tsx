import { Form } from "antd";
import React from "react";
import { useEffect } from "react";
import { useState } from "react";
import { AntInput } from "shared-resources/components";
import { v1 as uuid } from "uuid";
interface TimeRangeProps {
  value: any;
  filterValueChange: (value: any, type?: any) => void;
}

const TimeRangeComponent: React.FC<TimeRangeProps> = props => {
  const [val, setVal] = useState<any>();

  useEffect(() => {
    setVal(props.value["$lt"]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleTimeRangeChange = () => {
    props.filterValueChange({ $lt: val }, "age");
  };

  return (
    <Form.Item key={uuid()} label={"Time (in days)"} help={"Press Enter to update"}>
      <AntInput
        type="number"
        value={val}
        style={{ width: "100%" }}
        onChange={setVal}
        onPressEnter={handleTimeRangeChange}
      />
    </Form.Item>
  );
};

export default TimeRangeComponent;
