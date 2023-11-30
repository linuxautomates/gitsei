import React, { useCallback, useEffect, useMemo, useState } from "react";
import { v1 as uuid } from "uuid";

import "./WidgetPreview.scss";
import { AntCard, AntCol, AntIcon, AntRow, AntTag, AntTooltip, Button } from "../../../../shared-resources/components";
import { getParagraphFromOverview } from "./helper";
import CompactReport from "../../../../model/report/CompactReport";
import AuthImage from "../../../../shared-resources/components/auth-image/AuthImage";
import CreateWidgetModal from "../../../../configurable-dashboard/components/configure-widget/modal/CreateWidgetModal";
import { capitalize, get } from "lodash";
import { disableTooltipMessage } from "../report.constant";
import widgetConstants from "dashboard/constants/widgetConstants";
import { DEPRECATED_NOT_ALLOWED } from "dashboard/constants/applications/names";

interface WidgetPreviewProps {
  theme: CompactReport;
  selectedCategory: string;
  setWidgetInfoData: (value: CompactReport) => void;
}

const WidgetPreview: React.FC<WidgetPreviewProps> = ({ theme, selectedCategory, setWidgetInfoData }) => {
  const [showCreateModal, setVisibilityOfCreateModal] = useState(false);
  const [id, setId] = useState(uuid());
  const showWidgetCreateModal = useCallback(() => setVisibilityOfCreateModal(true), []);
  const hideWidgetCreateModal = useCallback(() => setVisibilityOfCreateModal(false), []);

  const _description = getParagraphFromOverview(id, theme?.description);
  const _categories = theme.categories.filter((key: string) => key.toLowerCase() != selectedCategory.toLowerCase());
  const reportType = get(theme, "key", theme?.key);
  const isDeprecatedAndNotAllowed = get(widgetConstants, [reportType, DEPRECATED_NOT_ALLOWED], false);

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
      // TODO remove listner
      if (elListner) {
        elListner.removeEventListener("click");
      }
    };
  }, []);

  const { supported_by_integration } = theme;

  const renderCreateModal = useMemo(() => {
    if (!showCreateModal) {
      return null;
    }
    return <CreateWidgetModal onClose={hideWidgetCreateModal} report={theme} />;
  }, [showCreateModal, hideWidgetCreateModal]);

  const reportContent = (disabled: boolean) => {
    return (
      <AntRow className="h-100 m-0 p-0 content-row" gutter={8}>
        <AntCol className="d-flex h-100" span={5}>
          <AuthImage className="widget-theme-preview__image" src={theme.imageUrl} alt={"report image"} />
        </AntCol>
        <AntCol span={19} className="widget-preview-content">
          <div className="title-info">
            <div className="title">{theme.name}</div>
            {disabled && (
              <AntTooltip placement="top" title={disableTooltipMessage}>
                <AntIcon type="info-circle" />
              </AntTooltip>
            )}
          </div>
          <div className="description">
            {_description}
            {_description && " "}
            {!theme?.hide_learn_more_button && (
              <button id={id} className="content-learn-more-button">
                Learn More
              </button>
            )}
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
    );
  };

  const disabledPreview = () => (
    <AntCard className="widget-theme-preview-disabled" onClick={showWidgetCreateModal}>
      {reportContent(true)}
    </AntCard>
  );

  const renderPreview = () => (
    <AntCard className="widget-theme-preview" onClick={showWidgetCreateModal}>
      {reportContent(false)}
    </AntCard>
  );

  if (isDeprecatedAndNotAllowed) {
    return disabledPreview();
  }
  if (!supported_by_integration) {
    return disabledPreview();
  }

  return (
    <>
      {renderPreview()}
      {renderCreateModal}
    </>
  );
};

export default React.memo(WidgetPreview);
