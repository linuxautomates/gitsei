import { filter, isArray, map } from "lodash";
import React, { useCallback, useMemo } from "react";
import { AntText, CustomTag, SvgIcon } from "shared-resources/components";

interface CustomTagSelectFilterProps {
  tag: Array<any> | string | undefined;
  onRemoveTag: (newTags: any) => void;
  onPopupIconClick: () => void;
}

const CustomTagSelectFilter: React.FC<CustomTagSelectFilterProps> = (props: CustomTagSelectFilterProps) => {
  const { tag, onRemoveTag, onPopupIconClick } = props;

  const onRemove = useCallback(
    (e: any, value: any) => {
      e?.preventDefault?.();
      if (isArray(tag)) {
        onRemoveTag(filter(tag || [], selected => selected.value !== value));
      } else {
        onRemoveTag(null);
      }
    },
    [tag]
  );

  const iconStyle = useMemo(() => ({ cursor: "pointer", width: 12, height: 12 }), []);

  const containerStyle = useMemo(
    () => ({
      width: "100%",
      borderBottom: "1px solid #aeadad"
    }),
    []
  );

  const innerContainerStyle = useMemo(
    () => ({ marginBottom: "0.25rem", overflow: "hidden", minHeight: "2rem", width: "100%" }),
    []
  );

  const iconsStyle = useMemo(
    () => ({
      width: !isArray(tag) && tag?.length ? "7%" : "4%",
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center"
    }),
    [tag]
  );

  return (
    <div style={containerStyle}>
      <div style={innerContainerStyle} className="flex justify-space-between align-center">
        <div className="flex" style={{ width: "93%", flexWrap: "wrap" }}>
          {isArray(tag) ? (
            <>
              {map(tag, selected => (
                <CustomTag label={selected.label} onRemove={(e: any) => onRemove(e, selected.value)} />
              ))}
            </>
          ) : (
            <AntText>{tag || ""}</AntText>
          )}
        </div>
        <div style={iconsStyle}>
          {!isArray(tag) && (tag || "")?.length > 0 && (
            <div onClick={(e: any) => onRemove(e, tag)}>
              <SvgIcon icon="close" style={iconStyle} />
            </div>
          )}
          <div onClick={onPopupIconClick}>
            <SvgIcon icon="view" style={iconStyle} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default React.memo(CustomTagSelectFilter);
