import "./attachment-item.style.scss";

import { Icon, Popconfirm } from "antd";
import React, { useEffect, useMemo, useState } from "react";
import { momentTimestampConvert } from "utils/timeUtils";

import { AntCol, AntRow, AntText, CustomSpinner, SvgIcon } from "..";
import { filePlaceholderIcon } from "../../../utils/fileUtils";
import SanitizedLink from "../SanitizedLInk";

interface AttachmentItemProps {
  fileName: string;
  previewFile?: () => void;
  removeFile?: () => void;
  trigger?: "click" | "icon";
  showBorder?: boolean;
  fileResponse?: any;
  loading?: boolean;
  type?: string;
}

export const AttachmentItem: React.FC<AttachmentItemProps> = ({
  fileName,
  previewFile,
  removeFile,
  trigger = "icon",
  showBorder = true,
  fileResponse,
  loading,
  type = "noLink"
}) => {
  const [showSVGIcon, setShowSVGIcon] = useState<boolean>(true);

  useEffect(() => {
    if (loading !== undefined && loading === false) {
      setShowSVGIcon(false);
      setTimeout(() => setShowSVGIcon(true), 1500);
    }
  }, [loading]);

  const fileIcon = useMemo(() => {
    if (!!fileName) {
      return (
        <span className="my-10 mx-5 file-icon" style={{ alignSelf: "flex-start" }}>
          {type === "noLink" && <SvgIcon icon={filePlaceholderIcon(fileName)} style={{ width: 24, height: 24 }} />}
          {type === "link" && <Icon className="file-icon__link" type="link" />}
        </span>
      );
    }
  }, [fileName]);

  if (
    ((loading === false || loading === undefined) && showSVGIcon && fileResponse?.user?.length > 0) ||
    type === "link"
  ) {
    return (
      <AntRow
        className={`files-details-container`}
        style={{
          height: "6rem",
          background: "#e7f7fe"
        }}>
        {fileIcon}
        <AntCol span={21}>
          <div className="flex direction-column">
            <div
              style={{
                fontSize: "14px",
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis"
              }}>
              {type === "noLink" && (
                <a
                  onClick={e => {
                    e.preventDefault();
                    previewFile && previewFile();
                  }}>
                  {fileName}
                </a>
              )}
              {type === "link" && <SanitizedLink url={fileName} />}
            </div>
            <div className="flex" style={{ margin: "6px 0 12px" }}>
              <div className="flex direction-column p-16 mr-50">
                <AntText style={{ color: "var(--grey3)" }}>
                  {type === "link" ? "Link Attached By" : "Uploaded By"}
                </AntText>
                {fileResponse.user || ""}
              </div>
              <div className="flex direction-column p-16">
                <AntText style={{ color: "var(--grey3)" }}>Timestamp</AntText>
                {momentTimestampConvert(fileResponse.created_at, "LLLL")}
              </div>
            </div>
          </div>
        </AntCol>
        <AntCol span={2} className="files-details-container-actions">
          <Popconfirm
            title={`Do you want to delete the file: ${fileName}?`}
            onConfirm={() => removeFile?.()}
            okText={"Yes"}
            cancelText={"No"}>
            <Icon type={"delete"} style={{ cursor: "pointer" }} />
          </Popconfirm>
        </AntCol>
      </AntRow>
    );
  }

  let _filePlaceholderIcon = !!fileName ? (
    <SvgIcon icon={filePlaceholderIcon(fileName)} style={{ width: 24, height: 24 }} />
  ) : (
    <></>
  );

  return (
    <AntRow className={`file-container ${showBorder ? "show-border" : ""}`}>
      {trigger === "icon" && (
        <>
          {loading ? (
            <CustomSpinner />
          ) : !showSVGIcon ? (
            <AntCol span={1} style={{ paddingLeft: "5px" }}>
              <SvgIcon icon={"tick"} />
            </AntCol>
          ) : (
            <AntCol span={1} style={{ paddingLeft: "5px" }}>
              <SvgIcon icon={filePlaceholderIcon(fileName)} style={{ width: 24, height: 24 }} />
            </AntCol>
          )}
        </>
      )}
      {trigger === "click" && (
        <>
          {loading ? (
            <CustomSpinner />
          ) : !showSVGIcon ? (
            <SvgIcon icon={"tick"} style={{ width: 24, height: 24 }} />
          ) : (
            <span style={{ marginRight: "5px" }}>{_filePlaceholderIcon}</span>
          )}
        </>
      )}
      <AntCol span={21}>
        <div
          style={{
            fontSize: "14px",
            whiteSpace: "nowrap",
            overflow: "hidden",
            textOverflow: "ellipsis"
          }}>
          {trigger === "click" && (
            <span
              style={{ cursor: "pointer" }}
              onClick={e => {
                e.preventDefault();
                previewFile && previewFile();
              }}>
              {fileName}
            </span>
          )}
          {trigger === "icon" && fileName}
        </div>
      </AntCol>
      <AntCol span={2} className="file-container-actions">
        {trigger === "icon" && (
          <Icon
            type={"eye"}
            onClick={e => {
              e.preventDefault();
              previewFile && previewFile();
            }}
          />
        )}
        {removeFile && (
          <Popconfirm
            title={`Do you want to delete the file: ${fileName}?`}
            onConfirm={() => removeFile()}
            okText={"Yes"}
            cancelText={"No"}>
            <Icon type={"delete"} className={"close-icon"} />
          </Popconfirm>
        )}
      </AntCol>
    </AntRow>
  );
};
