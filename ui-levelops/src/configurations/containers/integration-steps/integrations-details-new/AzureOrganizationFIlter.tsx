import React from "react";
import { Form, Input } from "antd";

interface AzureOrganizationFilterProps {
  organizations: string[];
  onOrganizationChange: (value: any) => void;
}

const AzureOrganizationFilter: React.FC<AzureOrganizationFilterProps> = ({ onOrganizationChange, organizations }) => {
  const handleOrganizationChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onOrganizationChange({ organizations: e.currentTarget.value });
  };

  return (
    <Form.Item label={"Collections"} colon={false} key={"Collections"}>
      <Input value={organizations} onChange={handleOrganizationChange} allowClear={true} />
    </Form.Item>
  );
};

export default AzureOrganizationFilter;
