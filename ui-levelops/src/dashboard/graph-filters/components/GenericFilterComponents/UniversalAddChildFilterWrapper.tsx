import { Icon } from "antd";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AntButton, AntIcon, AntTooltip } from "shared-resources/components";
import { toTitleCase } from "utils/stringUtils";

interface UniversalAddChildFilterWrapperProps {
    filterProps: LevelOpsFilter;
    handleAddOrderedFiltersChild: (key: string) => void;
}

const UniversalAddChildFilterWrapper: React.FC<UniversalAddChildFilterWrapperProps> = props => {

    const {
        filterProps,
        handleAddOrderedFiltersChild,
    } = props;

    const {
        childButtonLableName,
        parentKey,
        parentKeyData
    } = filterProps

    const INFO_MESSAGE = 'Please add parent filter value.';

    const lableName = useMemo(() => {

        if (childButtonLableName && typeof childButtonLableName === "function") {
            return childButtonLableName(filterProps?.beKey);
        } else if (childButtonLableName && typeof childButtonLableName === "string") {
            return toTitleCase(childButtonLableName);
        } else {
            return toTitleCase(filterProps.label);
        }
    }, [childButtonLableName, filterProps?.beKey])

    const allowAddFilterChild = useMemo(() => {
        if (parentKeyData && parentKeyData.length > 0) {
            return true;
        }
        return false;
    }, [parentKeyData]);

    return (
        <AntTooltip style={{ color: "#7E7E7E" }} title={!allowAddFilterChild ? INFO_MESSAGE : ''} >
            <AntButton
                style={allowAddFilterChild ? { color: "#2967dd" } : { color: "#7E7E7E" }}
                type="link"
                onClick={allowAddFilterChild ? () => handleAddOrderedFiltersChild(filterProps?.beKey) : ''}
            >
                <Icon type="plus-circle" />

                {lableName}

            </AntButton>
        </AntTooltip>
    )
};

export default UniversalAddChildFilterWrapper;
