import { Empty } from "antd";
import React from "react";
import { AntText } from "shared-resources/components";
interface ExpandedDetailsContainerProps {
  record: any;
}
const ExpandedDetailsContainer: React.FC<ExpandedDetailsContainerProps> = (props: ExpandedDetailsContainerProps) => {
  const renderContetnt = () => {
    if (Object.keys(props?.record.details).length) {
      return (
        <div>
          <AntText>Details</AntText>
          <div>{JSON.stringify(props.record?.details)}</div>
        </div>
      );
    }
    return <Empty />;
  };

  return renderContetnt();
};

export default ExpandedDetailsContainer;
