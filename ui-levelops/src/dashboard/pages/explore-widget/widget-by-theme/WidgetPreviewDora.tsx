import React, { useCallback, useEffect, useMemo, useState } from "react";
import { v1 as uuid } from "uuid";
import "./WidgetPreview.scss";
import {
  AntCard,
  AntCol,
  AntIcon,
  AntRow,
  AntTag,
  AntTooltip,
  Button,
  AntCheckbox
} from "../../../../shared-resources/components";
import { getParagraphFromOverview, getParagraphFromOverviewDora } from "./helper";
import CompactReport from "../../../../model/report/CompactReport";
import { capitalize, get } from "lodash";
import cx from "classnames";
import queryString from "query-string";
import { disableTooltipMessage, DISABLE_WIDGET_MESSAGE } from "../report.constant";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { useLocation } from "react-router-dom";
import { useParamSelector } from "reduxConfigs/selectors/selector";

interface WidgetPreviewDoraProps {
  theme: CompactReport;
  selectedCategory: string;
  setWidgetInfoData: (value: CompactReport) => void;
  isChecked: any;
  setIsChecked: any;
}

const WidgetPreviewDora: React.FC<WidgetPreviewDoraProps> = ({
  theme,
  selectedCategory,
  setWidgetInfoData,
  isChecked,
  setIsChecked
}) => {
  const [widgetCheckbox, setWidgetCheckbox] = useState<boolean>(false);
  const id = uuid();
  const location = useLocation();
  const _description = getParagraphFromOverviewDora(id, theme?.description);
  const _categories = theme?.categories?.filter((key: string) => key.toLowerCase() != selectedCategory.toLowerCase());
  const queryParamOU = queryString.parse(location.search).OU as string;
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU });

  const isDisabled = !workspaceProfile;
  useEffect(() => {
    const el = document.getElementById(id);
    let elListner: any;
    if (el) {
      elListner = el.addEventListener("click", (e: any) => {
        e.preventDefault();
        e.stopPropagation();
        setWidgetInfoData(theme);
      });
    }

    return () => {
      if (elListner) {
        elListner.removeEventListener("click");
      }
    };
  }, []);

  const { supported_by_integration } = theme;

  const onCheckboxValueChange = useCallback((event, theme: any) => {
    setWidgetCheckbox(event?.target?.checked);
    setIsChecked({ value: theme, checked: event?.target?.checked });
  }, []);

  const reportContent = (disabled: boolean) => {
    return (
      <AntTooltip title={disabled ? DISABLE_WIDGET_MESSAGE : null} placement="bottom">
        <AntRow className="h-100 m-0 p-0 content-row" gutter={8}>
          <AntCol span={24} className="widget-preview-content">
            <div className={"title-info-dora"}>
              <AntCheckbox
                className="mr-10"
                checked={widgetCheckbox}
                value={theme?.key}
                disabled={isDisabled} // needs to revisit when we have to write logic for disabling independent widget
                onChange={(e: any) => onCheckboxValueChange(e, theme)}></AntCheckbox>
              <div className="title">{theme.name}</div>
              {disabled && (
                <AntTooltip placement="top" title={disableTooltipMessage}>
                  <AntIcon type="info-circle" />
                </AntTooltip>
              )}
            </div>
            <div className="description-dora">
              {_description}
              {_description && " "}
            </div>
            <div className="theme-tags">
              {_categories.length > 0 &&
                _categories.map((category: string, key: number) => (
                  <AntTag key={`tag-${key}`}>
                    {(category || "")
                      .split("_")
                      .map((str: string) => capitalize(str))
                      .join(" ")}
                  </AntTag>
                ))}
            </div>
          </AntCol>
        </AntRow>
      </AntTooltip>
    );
  };

  const disabledPreview = useMemo(
    () => <AntCard className="widget-theme-preview-disabled">{reportContent(true)}</AntCard>,
    []
  );

  const renderPreview = useMemo(
    () => (
      <AntCard className={cx("widget-theme-preview", { "theme-border": widgetCheckbox === true })}>
        {reportContent(false)}
      </AntCard>
    ),
    [widgetCheckbox]
  );

  if (!supported_by_integration || isDisabled) {
    return disabledPreview;
  }

  return <>{renderPreview}</>;
};

export default React.memo(WidgetPreviewDora);
