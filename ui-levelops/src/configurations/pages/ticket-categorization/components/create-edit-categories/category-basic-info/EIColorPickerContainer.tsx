import React, { useState } from "react";
import { AntButton } from "shared-resources/components";
import ColorPicker from "shared-resources/components/color-picker/ColorPicker";
import { CATEGORY_DEFAULT_BACKGORUND_COLOR } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";

interface ColorPickerContainerProps {
  currentColor: string;
  colors: string[];
  handleOnColorSave: (value: string) => void;
  handleTogglePickersVisibility: (value: boolean) => void;
}

const EIColorPickerContainer: React.FC<ColorPickerContainerProps> = ({
  currentColor,
  colors,
  handleOnColorSave,
  handleTogglePickersVisibility
}) => {
  const [color, setColor] = useState<string>(currentColor ?? CATEGORY_DEFAULT_BACKGORUND_COLOR);

  const handleColorSelect = (value: string) => {
    setColor(value);
  };

  return (
    <div className="color-picker-container">
      <ColorPicker colors={colors} onClick={handleColorSelect} shapeClassName="circle-color" selectedColor={color} />
      <div className="footer">
        <AntButton onClick={(e: any) => handleTogglePickersVisibility(false)}>Cancel</AntButton>
        <AntButton type="primary" onClick={(e: any) => handleOnColorSave(color)}>
          Save
        </AntButton>
      </div>
    </div>
  );
};

export default EIColorPickerContainer;
