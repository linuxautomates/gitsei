import React, { useCallback, useMemo, useState } from "react";
import { Form, Popover } from "antd";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { categoryBasicInfo, ProfileBasicInfoType } from "../../types/ticketCategorization.types";
import EIColorPickerContainer from "../create-edit-categories/category-basic-info/EIColorPickerContainer";
import "./colorPickerWrapper.styles.scss";

const EIColorPickerWrapper: React.FC<{
  handleChanges: (key: categoryBasicInfo | ProfileBasicInfoType, value: string | number | object) => void;
  color: string;
  label?: string;
  filterKey: categoryBasicInfo | ProfileBasicInfoType;
  profile: RestTicketCategorizationScheme;
}> = ({ handleChanges, color, label, filterKey, profile }) => {
  const [colorPickerVisibility, setColorPickerVisibilty] = useState<boolean>(false);

  const handleTogglePickersVisibility = (value: boolean) => {
    setColorPickerVisibilty(value);
  };

  const handleOnColorSave = useCallback(
    (value: string) => {
      handleTogglePickersVisibility(false);
      handleChanges(filterKey, value);
    },
    [handleChanges]
  );

  const renderPopOverContent = useMemo(() => {
    return (
      <EIColorPickerContainer
        currentColor={color}
        colors={profile?.getSlicedCategoryColors()}
        handleOnColorSave={handleOnColorSave}
        handleTogglePickersVisibility={handleTogglePickersVisibility}
      />
    );
  }, [handleOnColorSave, handleTogglePickersVisibility, profile, color]);

  return (
    <Form.Item label={label ?? "Display Color"}>
      <div className="color-change-container">
        <div className="circle-color" style={{ backgroundColor: color }} />
        <div className="color-change">
          <p>{color}</p>
          <Popover placement="right" trigger="click" content={renderPopOverContent} visible={colorPickerVisibility}>
            <p className="open-color-picker-text" onClick={(e: any) => handleTogglePickersVisibility(true)}>
              Change
            </p>
          </Popover>
        </div>
      </div>
    </Form.Item>
  );
};

export default EIColorPickerWrapper;
