import React from "react";
import "./colorPicker.styles.scss";

interface ColorPickerProps {
  selectedColor: string;
  colors: string[];
  shapeClassName: string;
  onClick: (value: string) => void;
}

const ColorPicker: React.FC<ColorPickerProps> = ({ colors, shapeClassName, selectedColor, onClick }) => {
  return (
    <div className="color-picker">
      {colors.map(color => (
        <div
          className={shapeClassName}
          onClick={(e: any) => onClick(color)}
          style={{ backgroundColor: color, border: `${selectedColor === color ? "3px solid black" : ""}` }}
          key={color}
        />
      ))}
    </div>
  );
};

export default ColorPicker;
