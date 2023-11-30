import React, { useMemo } from "react";
import { TrackProps, HandleProps } from "react-ranger";
import { toTitleCase } from "utils/stringUtils";

interface SliderHandleProps {
  value: number;
  getHandleProps: <T>(props?: T) => T & HandleProps;
  active: boolean;
}

const SliderHandle: React.FC<SliderHandleProps> = (props: SliderHandleProps) => {
  const { value, getHandleProps, active } = props;

  const btnStyle = useMemo(
    () => ({
      appearance: "none",
      border: "none",
      background: "transparent",
      outline: "none"
    }),
    []
  );

  const handleStyle = useMemo(
    () => ({
      fontWeight: active ? 700 : 400,
      transform: active ? "translateY(-100%) scale(1.3)" : "translateY(0) scale(1)"
    }),
    [active]
  );

  const handleProps = useMemo(
    () => ({
      ...getHandleProps({
        style: btnStyle
      })
    }),
    [getHandleProps]
  );

  return (
    <button {...handleProps}>
      <div className="handle" style={handleStyle}>
        {value}
      </div>
    </button>
  );
};

export default SliderHandle;
