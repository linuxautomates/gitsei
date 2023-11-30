import React from "react";
import { Form, Typography } from "antd";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import { AntInput } from "shared-resources/components";
import "./CategoryBasicInfoComponent.style.scss";
import {
  categoryBasicInfo,
  CategoryBasicInfoTypes
} from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import EIColorPickerWrapper from "../../color-picker-wrapper/EIColorPickerWrapper";

interface CategoryBasicInfoProps {
  profile: RestTicketCategorizationScheme;
  category: RestTicketCategorizationCategory;
  handleChanges: (key: categoryBasicInfo, value: string | boolean) => void;
}

const CategoryBasicInfoComponent: React.FC<CategoryBasicInfoProps> = ({ category, profile, handleChanges }) => {
  return (
    <div className="category-basic-info-container">
      <Typography.Title level={4} className="basic-info-container-title">
        Basic Info
      </Typography.Title>
      <div className="basic-info-content-container">
        <Form colon={false}>
          <Form.Item label="Name" required>
            <AntInput
              defaultValue={category?.name}
              onChange={(e: any) => handleChanges("name", e?.target?.value)}
              value={category?.name}
            />
          </Form.Item>
          <Form.Item label="Description">
            <AntInput
              defaultValue={category?.description}
              value={category?.description}
              onChange={(e: any) => handleChanges("description", e?.target?.value)}
            />
          </Form.Item>
          <EIColorPickerWrapper
            handleChanges={handleChanges as any}
            color={category?.background_color ?? ""}
            filterKey={CategoryBasicInfoTypes.BACKGROUND_COLOR}
            profile={profile}
          />
        </Form>
      </div>
    </div>
  );
};

export default CategoryBasicInfoComponent;
